# Bruker Chainguard secure base image, https://sikkerhet.nav.no/docs/sikker-utvikling/baseimages


FROM debian:13-slim AS locale

RUN set -eux; \
	apt-get update; apt-get install -y --no-install-recommends locales; \
	echo 'nb_NO.UTF-8 UTF-8' >> /etc/locale.gen; \
	locale-gen; \
	locale -a | grep 'nb_NO.utf8'

FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-21

# To support Norwegian characters in the flyway-script filenames
COPY --from=locale /usr/lib/locale /usr/lib/locale

COPY /app/build/libs/app-all.jar /app/app.jar

WORKDIR /app

ENV LANG='nb_NO.UTF-8' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"
# Kommentar til bruk av XX:ActiveProcessorCount:
# Dette påvirker kode som har logikk basert på JVM-metoden Runtime.getRuntime().availableProcessors()
# Uten limit i Kubernetes returnerer den antall CPU i noden, som kan være mye høyere enn det som er tildelt pod'en.
# Dette kan føre til at applikasjonen prøver å bruke flere tråder enn det som er optimalt for pod'en.
# Nå returnerer metoden det tallet vi angir istedenfor.
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=2"

CMD ["java", "-jar", "app.jar"]