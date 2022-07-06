Changelog
=========

v3.1.0 (2022/06/07)
-------------------

* Changed build target to JDK 1.7 to enable use of plugin on older servers

v3.0.0 (2022/06/27)
-------------------

* Added support for 1.16+ RGB colors in greentext

v2.2.2 (2020/02/14)
-------------------

* Fixed update checker to actually check for updates
* Fixed orangetext routine to actually check for permissions
* Moved update checking to post-init
* Removed useless plugin-specific debug option

v2.2.1 (2020/01/01)
-------------------

* Changed update checker to not print stack trace on fail

v2.2.0 (2019/08/03)
-------------------

* Added automatic update checker
* Separated exceptions for greentext and orangetext
* Stopped greenifier from greenifying empty messages

v2.1.0 (2019/07/15)
-------------------

* Added persistent map of player status
* Added message if player is already enabled/disabled
* Added command aliases: `bgt`, `bukkitgreentext`
* Re-wrote command parser to be more efficient


v2.0.0 (2019/07/05)
-------------------

* Initial (re-)release
