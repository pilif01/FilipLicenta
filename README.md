```mermaid
classDiagram
    direction LR
    actor User
    rectangle "OCR - TOOL Application" {
        usecase "Login" as UC1
        usecase "Register User Account" as UC2
        usecase "Parse XML to Excel (Warnings)" as UC3
        usecase "Create Warning Excel (Filtered)" as UC4
        usecase "Create Image Excel (Filtered)" as UC5
        usecase "Perform Manual Warning Testing" as UC6
        usecase "Perform Manual Icon Testing" as UC7
        usecase "Use File Renaming Tool" as UC8
        usecase "Perform Automated Icon Testing" as UC9
        usecase "Perform Automated Warning Testing" as UC10
        usecase "Select Files/Folders" as UC11
        usecase "Select Crop Area" as UC12
        usecase "Display Log" as UC13
        usecase "Save Results" as UC14
        usecase "Authenticate User" as UC15
        usecase "Register User in DB" as UC16
        usecase "Connect to Database" as UC17_DB
        usecase "Segment Thai Text" as UC18
        usecase "Enhance Image for OCR" as UC19
        usecase "Compare Images (Pixel to Pixel)" as UC20
        usecase "Perform OCR" as UC21
        usecase "Configure Excel Columns" as UC22
        usecase "Log Application Events" as UC23_Log
        usecase "Set UI Font" as UC24
        usecase "Manage User Session" as UC25
        usecase "Rename Files (Add/Remove Prefix/Suffix)" as UC26_Rename
    }
    User --|> UC1
    User --|> UC2
    User --|> UC3
    User --|> UC4
    User --|> UC5
    User --|> UC6
    User --|> UC7
    User --|> UC8
    User --|> UC9
    User --|> UC10
    UC1 ..> UC15 : includes
    UC1 ..> UC17_DB : uses
    UC2 ..> UC16 : includes
    UC2 ..> UC17_DB : uses
    UC3 ..> UC11 : includes
    UC3 ..> UC13 : uses
    UC4 ..> UC11 : includes
    UC4 ..> UC13 : uses
    UC5 ..> UC11 : includes
    UC5 ..> UC13 : uses
    UC6 ..> UC11 : includes
    UC6 ..> UC13 : includes
    UC6 ..> UC14 : includes
    UC6 ..> UC22 : includes
    UC6 ..> UC18 : extends
    UC6 ..> UC19 : extends
    UC7 ..> UC11 : includes
    UC7 ..> UC13 : includes
    UC7 ..> UC14 : includes
    UC7 ..> UC22 : includes
    UC8 ..> UC11 : includes
    UC8 ..> UC13 : includes
    UC8 ..> UC26_Rename : includes
    UC9 ..> UC11 : includes
    UC9 ..> UC12 : includes
    UC9 ..> UC13 : includes
    UC9 ..> UC14 : includes
    UC9 ..> UC20 : includes
    UC10 ..> UC11 : includes
    UC10 ..> UC12 : includes
    UC10 ..> UC13 : includes
    UC10 ..> UC14 : includes
    UC10 ..> UC18 : includes
    UC10 ..> UC19 : includes
    UC10 ..> UC21 : includes
    UC15 .up.> UC25 : extends
    UC16 .up.> UC25 : extends
    UC24 .d.> UC1 : extends
    UC24 .d.> UC2 : extends
    UC24 .d.> UC25 : extends
    UC23_Log .d.> UC3 : uses
    UC23_Log .d.> UC4 : uses
    UC23_Log .d.> UC5 : uses
    UC23_Log .d.> UC6 : uses
    UC23_Log .d.> UC7 : uses
    UC23_Log .d.> UC8 : uses
    UC23_Log .d.> UC9 : uses
    UC23_Log .d.> UC10 : uses
    UC23_Log .d.> UC1 : uses
    UC23_Log .d.> UC2 : uses
    UC23_Log .d.> UC25 : uses
    UC26_Rename ..> UC11 : includes
