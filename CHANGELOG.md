**This release contains a lot of fixes, changes, and a few feature additions. I would HIGHLY recommend upgrading to this verison if you are using any type of SQL storage method.**


## Changes
- SQLite is now the default storage method for BreweryX. For servers that experience slow saving times with `FlatFile` storage, you should migrate to SQLite, MySQL, or MongoDB. **~ by Nadwey**
  `->` To migrate, start your server using the storage method `FlatFile`. Next, change your storage method to `SQLite` or another database storage method. Finally, run: `/brew data reload` and `/brew data save` to finalize your migration.


## Additions
- You can now configure barrel inventory sizes **~ by Mitality**
- BreweryX now hooks into the [Lands](https://www.spigotmc.org/resources/53313) plugin. **~ by Mitality**
- You can now configure your own custom translations in BreweryX **~ by Thorinwasher**
- Added a Ukranian translation **~ by Thorinwasher**


### Fixes
- Fixes SQL storage methods not deleting objects from the database. If you're using an older version of BreweryX with an SQL database, you should update! **~ by Jsinco**
- Fixes Brewery not recognizing `cauldron.yml` recipes, [reference here](https://discord.com/channels/1108854517048549396/1331903625655615509/1331903625655615509) **~ by Jsinco**


**That's it, Github generated release notes are below. Have a good day and go get your new version from [Modrinth](https://modrinth.com/plugin/breweryx/version/gbkMRZcU)**


## What's Changed
* Set SQLite as the default storage type by @Nadwey in https://github.com/BreweryTeam/BreweryX/pull/96
* Fix: SQL saving, Cauldron brew matching by @Jsinco in https://github.com/BreweryTeam/BreweryX/pull/105
* Fix lore of sealed potions by @Nadwey in https://github.com/BreweryTeam/BreweryX/pull/95
* Allow custom translations by @Thorinwasher in https://github.com/BreweryTeam/BreweryX/pull/103
* Ukrainian translation by @Thorinwasher in https://github.com/BreweryTeam/BreweryX/pull/102


**Full Changelog**: https://github.com/BreweryTeam/BreweryX/compare/3.4.9...3.4.10

<!-- Auto-update: 2025-10-06T19:54:05.242023 -->

<!-- Auto-update: 2025-10-27T08:11:25.703565 -->
