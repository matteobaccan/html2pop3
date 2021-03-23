del keystore.pfx
keytool.exe -genkey -dname "CN=Matteo Baccan, L=Novara, ST=ST, C=US, OU=, O=https://www.baccan.it" -alias html2pop3 -keyalg RSA -destkeystore keystore.pfx -keysize 2048 -storetype pkcs12 -storepass password
