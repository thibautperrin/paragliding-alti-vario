# paragliding-alti-vario
Android Wear application to measure the vertical speed while paragliding

Tested on Galaxy watch 4.

As a user I can :
- Compile and run the app on my Android Wear watch
- Accept runtime permission manually
- Go the take-off place
- Launch the app and wait to have the GPS fix (Speed and elevation will be visible)
- Click on START
- Look at the screen's color to know their vertical speed, or watch directly at the elevation speed value in m/s.
- Click on STOP at the end of the fly
- Open Android studio, and on the right, open the Device file explorer
- Retrieve all the data files in /data/data/com.android.thibautperrin.parapente/files/
- Transform the raw data "<date>_location.file" into a GPX file using the dedicated androidTest.
