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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

/**
 * Generate license with truelicense API.
 *
 * @author BOUCHAAR tareq
 */
public class LicenseGenerator {

    //################ Requirements (Prérequis) ######################
    //
    // Generate private and public keys in src/main/resources
    //
    // keytool -genkey -alias dcPrivateKey -keyalg RSA -keystore dcPrivateKey.store -keysize 2048
    // keytool -export -alias dcPrivateKey -file certfile.cer -keystore dcPrivateKey.store
    // keytool -import -alias dcPublicKey -file certfile.cer -keystore dcPublicKey.store
    // rm certfile.cer
    // cp dcPrivateKey.store src/main/resources
    // cp dcPublicKey.store src/main/resources
    //
    //#################################################################
    private static final String PROPERTIES_FILENAME = "LicenseGenerateConfig.properties";

    private String appName;
    private String licenseFileExtension;
    private String firstName;
    private String lastName;
    private String city;
    private String state;
    private String country;

    private String keystoreFilename;     // this app needs the "private" keystore
    private String keystorePassword;
    private String keyPassword;
    private String alias;
    private String cipherParamPassword;  // 6+ chars, and both letters and numbers

    public static void main(String[] args) {

        LicenseGenerator licenseGen = new LicenseGenerator();

        File licenseFile = licenseGen.createLicenseFile("license");

        System.out.println("License created : " + licenseFile.getAbsolutePath());

    }

    public LicenseGenerator() {
        // load license config properties
        loadLicenseGeneratePropertiesFile();
    }

    public File createLicenseFile(final String fileBasename) {

        LicenseParam licenseParam = getLicenseParam();

        try {
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED,
                                    (TransformParameterSpec) null)), null, null);

            SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                    (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    Collections.singletonList(ref));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // create the license file
        LicenseManager lm = new LicenseManager(licenseParam);
        try {
            // write the file to the same directory we read it in from
            File file = new File(fileBasename + licenseFileExtension);
            LicenseContent licenseContent = createLicenseContent(licenseParam);
            lm.store(licenseContent, file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadLicenseGeneratePropertiesFile() {

        Properties properties = new Properties();

        ClassLoader classLoader = LicenseGenerator.class
                .getClassLoader();
        File file = new File(classLoader.getResource(PROPERTIES_FILENAME).getFile());

        try (FileInputStream in = new FileInputStream(file)) {

            properties.load(in);

            appName = properties.getProperty("app_name");
            licenseFileExtension = properties.getProperty("license_file_extension");
            keystoreFilename = properties.getProperty("keystore_filename");
            keystorePassword = properties.getProperty("keystore_password");
            alias = properties.getProperty("alias");
            keyPassword = properties.getProperty("key_password");
            cipherParamPassword = properties.getProperty("cipher_param_password");

            firstName = properties.getProperty("first_name");
            lastName = properties.getProperty("last_name");
            city = properties.getProperty("city");
            state = properties.getProperty("state");
            country = properties.getProperty("country");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Set up the LicenseContent instance. This is the information that will be in the
    // generated license file.
    private LicenseContent createLicenseContent(LicenseParam licenseParam) {
        LicenseContent result = new LicenseContent();
        X500Principal holder = new X500Principal("CN=" + firstName + " " + lastName + ", "
                + "L=" + city + ", "
                + "ST=" + state + ", "
                + "C=" + country);
        result.setHolder(holder);
        X500Principal issuer = new X500Principal(
                "CN=BOUCHHAR Tareq, L=Baba Hassen, ST=Alger, "
                + " OU=FreeLance Developpement,"
                + " O=TBO,"
                + " C=Algérie,"
                + " DC=DZ");
        result.setIssuer(issuer);
        result.setConsumerAmount(1);
        result.setConsumerType("User");
        result.setInfo("License key for the " + appName + " application for trial version.");
        Date now = new Date();
        result.setIssued(now);
        System.out.println("---- Date = " + now);
        //now.setYear(now.getYear() + 50);
        Calendar cal = Calendar.getInstance(); // creates calendar
        cal.setTime(now); // sets calendar time/date
        cal.add(Calendar.MINUTE, 1); // adds one day
        result.setNotAfter(cal.getTime());

        result.setSubject(licenseParam.getSubject());
        return result;
    }

    private LicenseParam getLicenseParam() {

        // set up an implementation of the KeyStoreParam interface that returns
        // the information required to work with the keystore containing the private key:
        final KeyStoreParam privateKeyStoreParam = new KeyStoreParam() {
            public InputStream getStream() throws IOException {
                final String resourceName = keystoreFilename;
                final InputStream in = LicenseGenerator.class.getClassLoader().getResourceAsStream(resourceName);
                if (in == null) {
                    System.err.println("Could not load file: " + resourceName);
                    throw new FileNotFoundException(resourceName);
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
                return keyPassword;
            }
        };

        // Set up an implementation of the CipherParam interface to return the password to be
        // used when performing the PKCS-5 encryption.
        final CipherParam cipherParam = new CipherParam() {
            public String getKeyPwd() {
                return cipherParamPassword;
            }
        };

        // Set up an implementation of the LicenseParam interface.
        // Note that the subject string returned by getSubject() must match the subject property
        // of any LicenseContent instance to be used with this LicenseParam instance.
        LicenseParam licenseParam = new LicenseParam() {
            public String getSubject() {
                return appName;
            }

            public Preferences getPreferences() {
                // TODO why is this needed for the app that creates the license?
                //return Preferences.userNodeForPackage(LicenseGenerator.class);
                return null;
            }

            public KeyStoreParam getKeyStoreParam() {
                return privateKeyStoreParam;
            }

            public CipherParam getCipherParam() {
                return cipherParam;
            }
        };
        return licenseParam;
    }

}
