# Personal Page

## Installation

1. `npm install --global gulp-cli` - to install gulp globally; (If you have previously installed a version of gulp globally, please run `npm rm --global gulp` to make sure your old version doesn't collide with gulp-cli)

2. `npm install` - to install the dependencies;

3. Install NGINX;

4. Add the following configuration file to NGINX:
  * `diegourban.github.io.conf` - is the nginx configuration file, it defines two servers to evaluate both versions of the page: src and dist;

## Usage

#### Folder structure:
* `/src` - contains the personal page source code based on the original theme and adapted to my needs;

Make the changes at `/src` folder, run `gulp` to copy and minify the content from src to dist

* `http://localhost:4040` - to src version;
* `http://localhost:5050` - to dist version;

`gulp update` - to update the repo files from dist.
