# Personal Page source

Contains the source code from my personal page. It is adapted over a theme that I've bought online.

## Installation

1. `npm install --global gulp-cli` - to install gulp globally; (If you have previously installed a version of gulp globally, please run `npm rm --global gulp` to make sure your old version doesn't collide with gulp-cli)

2. `npm install` - to install the dependencies;

3. Install NGINX;

4. Add the following configuration file to NGINX:
  * `diegourban.github.io.conf` - is the nginx configuration file, it define two servers to evaluate both versions(src | dist) of the page;

## Usage

#### Folder structure:
* `/MStone-1.4.2` - contains the original theme;
* `/src` - contains the personal page source code based on the original thame and adapted to my needs;

Make the changes at `/src` folder, run `gulp copy` to copy the content from src to dist and evaluate with

* `http://localhost:4040` - to src version;
* `http://localhost:5050` - to dist version;

when the changes are ready, use:

`deploy.bat` or `./deploy.sh` - to deploy the changes from this source to [diegourban.github.io host repository](https://github.com/diegourban/diegourban.github.io);

The deploy routine will clone the host repo, clean it, copy the source files, commit and push the changes. It will prompt you to inform the password and confirm actions.

Updating the host repository will automatically update the github page(https://diegourban.github.io).
