#!/bin/sh

git_setup() {
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "Travis CI"
}

copy_file() {
    chmod +x doc/locales/en/LC_MESSAGES/*.po
    chmod +x doc/locales/zh/LC_MESSAGES/*.po
    cp -rf doc/locales/en/LC_MESSAGES/*.po build_doc_po_file/bigflow/doc/locales/en/LC_MESSAGES/
    cp -rf doc/locales/zh/LC_MESSAGES/*.po build_doc_po_file/bigflow/doc/locales/zh/LC_MESSAGES/
    cd build_doc_po_file/bigflow
}

git_add() {
    git checkout doc
    git add doc/locales/en/LC_MESSAGES/*.po
    git add doc/locales/zh/LC_MESSAGES/*.po
    git commit -m "Update po files: $TRAVIS_BUILD_NUMBER"
}

git_push() {
    git remote add origin-doc https://$GITHUB_TOKEN@github.com/yshysh/bigflow.git
    git push --set-upstream origin-doc doc
}

git_setup
copy_file
git_add
git_push
