Instrument Inteligent de Recunoaștere Optică a Caracterelor (OCR-Tool)

Pentru a rula aceasta aplicatie este necesar:

-Instalarea JDK 17

-Descarcarea Executabilului OCR-Tool

-Logarea cu userul generic : user:filip; pass:123

Pentru a testa functionalitatile aplicatiei am sa atasez pe github un document cu niste poze si un XML care pot fi testate

Fisierul atasat se numeste DemoLicenta si contine urmatoarele:

-Text_Warnings.xml

-dwn_pics

-icon_down_pics

-icon_ref_pics

-tessdata

Pentru a testa aplicatia trebuie urmati urmatorii pasi:

1. Login cu userul generic

2. Parse XML file: Selectati fisierul excel din folderul DemoLicenta si dupa terminarea parsarii trebuie sa se acceseze excelul pentru a seleca ID-ul testat. Pozele sunt doar pentru ID-ul 0x200002, in dreptul acestuia pe coloana TControl trebuie pus "RUN" iar la celelalte ID-uri din lista trebie pus "SKIP"

3. Creeare Excel pentru testare de texte/pictograme

4. Testare full auto (selectand documentele Excel creeate la punctul 3). De mentionat faptul ca pozele din DemoLicenta se incadreaza la "TwinTube", deci cand se va configura zona de decupare va trebui selectat butonul "TwinTube"

5. Testare Manuala (selectand documentele Excel creeate la punctul 4).
