var gulp = require('gulp'),
	clean = require('gulp-clean'),
	imagemin = require('gulp-imagemin'),
	cssnano = require('gulp-cssnano'),
	uglify = require('gulp-uglify'),
	htmlmin = require('gulp-htmlmin');

gulp.task('clean', function() {
	return gulp.src('dist').pipe(clean());
});

gulp.task('copy', ['clean'], function() {
	return gulp.src('src/**/*').pipe(gulp.dest('dist'));
});

gulp.task('minify-css', function() {
	gulp.src('dist/assets/**/*.css')
    	.pipe(cssnano({safe: true}))
    .	pipe(gulp.dest('dist/assets'))
});

gulp.task('minify-js', function() {
	gulp.src('dist/assets/**/*.js')
    	.pipe(uglify())
    	.pipe(gulp.dest('dist/assets'))
});

gulp.task('minify-html', function() {
	gulp.src('dist/**/*.html')
    	.pipe(htmlmin({collapseWhitespace: true}))
    	.pipe(gulp.dest('dist/'))
});

gulp.task('minify-img', function() {
	gulp.src('dist/assets/images/**/*')
		.pipe(imagemin())
		.pipe(gulp.dest('dist/assets/images'));
});

gulp.task('minify', ['minify-css', 'minify-js', 'minify-html', 'minify-img']);

gulp.task('default', ['copy'], function() {
	gulp.start('minify');
});

gulp.task('clean-repo', function() {
	return gulp.src(['index.html', 'favicon.ico', 'assets']).pipe(clean());
});

gulp.task('update', ['clean-repo'], function() {
	return gulp.src('dist/**/*').pipe(gulp.dest(''));
});
