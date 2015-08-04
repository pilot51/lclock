# lclock
L-Clock is an Android app that fetches data from SpaceflightNow.com and creates a formatted list of upcoming rocket launches.


List of changes from base app:

-Major UI overhaul, material design, added images for rockets

-Refactored names of classes including event>launch for clarity

-Fixed minor parsing error with &

-Moved downloading of file to AsyncTask

-Changed displayed time to match users timezone

-Added individual notifications for each launch (some minor bugs still)

-Moved countdown timer to detailed activities

-Added about section to settings

-Some changes to base files caused by using android studio

-Minimum SDK changed to 16, target SDK 22


TODO:

-Move strings to xml file

-More settings, dark theme

-Add more rocket images
