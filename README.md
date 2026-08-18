# SafeScan Android

SafeScan is een lokale Android-beveiligingsscanner met herstelbegeleiding, backup/restore, uitgebreide PDF-rapportage en een AndroidQF/MVT-forensische workflow.

## Downloads

- [SafeScan.apk](SafeScan.apk)
- [Windows-helper](forensics/install-and-scan.ps1)
- [Linux-helper](forensics/install-and-scan.sh)
- [Forensische instructies](forensics/README.md)

## AndroidQF + MVT

AndroidQF verzamelt forensische gegevens; MVT analyseert daarna mogelijke sporen van spyware. Gebruik dit uitsluitend op je eigen toestel of met uitdrukkelijke toestemming.

Linux:

```bash
cd forensics
./install-and-scan.sh
```

Windows PowerShell (met WSL2, ADB en AndroidQF):

```powershell
cd forensics
Set-ExecutionPolicy -Scope Process Bypass
.\install-and-scan.ps1
```

Typ `JA` wanneer daarom wordt gevraagd. De uitvoer staat in `safescan-forensics-*`.

Officiële bronnen: [AndroidQF](https://github.com/mvt-project/androidqf) · [MVT](https://github.com/mvt-project/mvt) · [Android-methode](https://github.com/mvt-project/mvt/blob/main/docs/android/methodology.md)

## Belangrijk

SafeScan wijzigt beveiligingsinstellingen niet stilzwijgend. Maak eerst een backup; herstelacties openen de officiële Android-instellingen voor handmatige bevestiging. AndroidQF en MVT draaien op een computer met ADB en verzamelen/analyseren alleen met toestemming.

## Privacy

Er zijn geen screenshots met toestelgegevens opgenomen. Exporteer of deel rapporten alleen nadat je modelnaam, Android-ID, accounts en andere identificerende gegevens hebt gecontroleerd.
