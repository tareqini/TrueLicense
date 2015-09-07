package com.tareqini.licensegen;

import de.schlichtherle.license.CipherParam;
import de.schlichtherle.license.KeyStoreParam;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import de.schlichtherle.license.LicenseParam;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Install and verify license.
 *
 * @author BOUCHAAR tareq
 */
public class LicenseController {

    private static final String PROPERTIES_LICENSE_INSTALL_FILENAME = "LicenseInstallConfig.properties";

    private String appName;
    private String keystoreFilename;
    private String keystorePassword;
    private String alias;
    private String cipherParamPassword;

    public LicenseController() {
        loadLicenseInstallPropertiesFile();
    }

    public static void main(String[] args) throws Exception {

        String licenseFileName = "/home/tareq/NetBeansProjects/TrueLicense/license.lic";

        LicenseController licenseController = new LicenseController();
        //install license file
        LicenseContent license = licenseController.installLicense(licenseFileName);
        System.out.println("License isntaled successfuly : " + license.getSubject() + " " + license.getInfo());
        System.out.println("                      Holder : " + license.getHolder().toString());
        System.out.println("                      Issuer : " + license.getIssuer().toString());
        System.out.println("                   Generated : " + license.getIssued());
        System.out.println("                      Expire : " + license.getNotAfter());
        //verify installed license
        licenseController.verifyLicense();
    }

    public LicenseContent verifyLicense() throws Exception {

        LicenseParam licenseParam = getLicenseParam();
        try {
            LicenseManager lm = new LicenseManager(licenseParam);

            LicenseContent licenseContent = lm.verify();

            return licenseContent;

        } finally {
            try {
                licenseParam.getKeyStoreParam().getStream().close();
            } catch (IOException ex) {

            }
        }

    }

    /**
     * Install the license file.
     *
     * @return LicenseContent if the license installed properly, null otherwise.
     */
    public LicenseContent installLicense(String licenseFilename) throws Exception {

        LicenseParam licenseParam = getLicenseParam();
        try {
            LicenseManager lm = new LicenseManager(licenseParam);

            lm.uninstall();

            File licenseFile = new File(licenseFilename);
            LicenseContent licenseContent = lm.install(licenseFile);

            return licenseContent;
        } finally {
            try {
                licenseParam.getKeyStoreParam().getStream().close();
            } catch (IOException ex) {

            }
        }
    }

    private LicenseParam getLicenseParam() {

        //implementation of KeyStoreParam interface
        // required the keystore containing the private key
        final KeyStoreParam publicKeyStoreParam = new KeyStoreParam() {
            public InputStream getStream() throws IOException {
                final InputStream in = LicenseController.class
                        .getClassLoader().getResourceAsStream(keystoreFilename);
                if (in
                        == null) {
                    System.err.println("Could not load file: " + keystoreFilename);
                    throw new FileNotFoundException(keystoreFilename);
                }
                return in;
            }

            public String getAlias() {
                return alias;
            }

            public String getStorePwd() {
                return keystorePassword;
            }

            public String getKeyPwd() {
                return null;
            }
        };

        final CipherParam cipherParam = new CipherParam() {
            public String getKeyPwd() {
                return cipherParamPassword;
            }
        };

        return new LicenseParam() {
            public String getSubject() {
                return appName;
            }

            public Preferences
                    getPreferences() {

                return Preferences.userNodeForPackage(LicenseController.class);
//                return null;
            }

            public KeyStoreParam getKeyStoreParam() {
                return publicKeyStoreParam;
            }

            public CipherParam getCipherParam() {
                return cipherParam;
            }
        };
    }

    private void loadLicenseInstallPropertiesFile() {

        Properties properties = new Properties();

        ClassLoader classLoader = LicenseController.class
                .getClassLoader();
        File file = new File(classLoader.getResource(PROPERTIES_LICENSE_INSTALL_FILENAME).getFile());

        try (FileInputStream in = new FileInputStream(file)) {

            properties.load(in);

            appName = properties.getProperty("app_name");
            keystoreFilename = properties.getProperty("keystore_filename");
            keystorePassword = properties.getProperty("keystore_password");
            alias = properties.getProperty("alias");
            cipherParamPassword = properties.getProperty("cipher_param_password");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
