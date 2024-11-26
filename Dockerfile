# Kan vurdere å bruke 12-slim, men den er foreløpig eksprimentell, ref: https://hub.docker.com/_/debian
FROM debian:12 as locale

# Sakset og tilpasset til NO fra https://github.com/docker-library/postgres/blob/master/17/bookworm/Dockerfile
# make the "nb_NO.UTF-8" locale so app will be utf-8 enabled by default
RUN set -eux; \
	if [ -f /etc/dpkg/dpkg.cfg.d/docker ]; then \
# if this file exists, we're likely in "debian:xxx-slim", and locales are thus being excluded so we need to remove that exclusion (since we need locales)
		grep -q '/usr/share/locale' /etc/dpkg/dpkg.cfg.d/docker; \
		sed -ri '/\/usr\/share\/locale/d' /etc/dpkg/dpkg.cfg.d/docker; \
		! grep -q '/usr/share/locale' /etc/dpkg/dpkg.cfg.d/docker; \
	fi; \
	apt-get update; apt-get install -y --no-install-recommends locales; rm -rf /var/lib/apt/lists/*; \
	echo 'nb_NO.UTF-8 UTF-8' >> /etc/locale.gen; \
	locale-gen; \
	locale -a | grep 'nb_NO.utf8'

FROM gcr.io/distroless/java21-debian12:nonroot

COPY --from=locale /usr/lib/locale /usr/lib/locale

WORKDIR /app
COPY /app/build/libs/app-all.jar /app/app.jar

ENV LANG='nb_NO.UTF-8' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=2"

CMD ["app.jar"]

# use -XX:+UseParallelGC when 2 CPUs and 4G RAM.
# use G1GC when using more than 4G RAM and/or more than 2 CPUs
# use -XX:ActiveProcessorCount=2 if less than 1G RAM.
