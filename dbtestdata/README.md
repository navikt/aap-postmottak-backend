# dbtest
Har ansvaret for kode som anvendes i testkode på tvers av flere moduler.

Når flere moduler har behov for å teste databasen, uten at de kan ha 
avhengighet til selve applikasjonen, så trenger vi noe delt på kode på tvers.

Denne modulen tilbyr kode som kan anvedes i andre moduler sin testkode - viktig 
å derfor ikke inkludere kode som skal anvendes i kjørekoden (ikke testkode) da
denne modulen kun bør inkluderes som en test-dependency for å unngå og trekke inn
testkode og testavhengigheter i kjørekoden.