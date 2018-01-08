#!/bin/sh

git_setup() {
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "Travis CI"
}

copy_file() {
    cp -rf doc/locales build_doc_po_file/doc
    cd build_doc_po_file
}

git_add() {
    git checkout -b doc
    git add doc/locales/en/LC_MESSAGES/*.po
    git add doc/locales/zh/LC_MESSAGES/*.po
    git commit -m "Update po files: $TRAVIS_BUILD_NUMBER"
}

git_push() {
    git remote add origin-doc https://$GITHUB_TOKEN@github.com/yshysh/bigflow.git
    git push --set-upstream origin-doc doc
}

git_setup
#copy_file
git_add
git_push
