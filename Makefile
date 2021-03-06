install:
	mvn clean install

site:
	mvn site

	# fix refs to JXR line numbers
	find target/site -name '*.html' -print0 | xargs -0 sed -i 's:\(\.html#\)L\([[:digit:]]\):\1\2:g'

	git fetch origin +gh-pages:gh-pages
	rm -rf target/site-repo
	git clone . target/site-repo -b gh-pages --single-branch
	cp -a target/site/. target/site-repo/
	cd target/site-repo && git add -A && git commit -m 'updated site' && git push

site-deploy:
	git push origin gh-pages

gitsite:
	mvn clean install site site-deploy

codeship-setup:
	# maven repository will be cached between builds - it has no snapshots anyway
	rm -rf .m2/repository
	mkdir -p .m2 cache/repository
	ln -sf ../cache/repository .m2/repository || true
	#
	mvn dependency:go-offline

codeship-test:
	mvn --batch-mode --show-version -e verify
