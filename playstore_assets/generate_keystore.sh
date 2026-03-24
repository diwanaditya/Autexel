#!/bin/bash
# Run this script to generate your signing keystore
# SAVE the .jks file and passwords somewhere SAFE — you can never recover them!

echo "=== Autexel Keystore Generator ==="
echo "You will be asked for a password and your details."
echo ""

keytool -genkey -v \
  -keystore autexel_release_key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias autexel_key \
  -dname "CN=Autexel, OU=App, O=YourCompany, L=YourCity, S=YourState, C=IN"

echo ""
echo "✅ Keystore created: autexel_release_key.jks"
echo "⚠️  BACKUP THIS FILE! If lost, you cannot update your app on Play Store."
