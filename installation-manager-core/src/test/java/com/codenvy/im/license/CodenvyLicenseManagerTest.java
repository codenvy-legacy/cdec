/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.license;

import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static org.testng.Assert.assertEquals;


/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class CodenvyLicenseManagerTest {

    private static final String INVALID_LICENCE_TEXT = "some license";

    // Custom Signed Features: | type = product key | expiration = 2020/12/01 | users = 10 |
    private static final String LICENCE_TEXT = "1c4594dd37a0a02e8522c0c7670431dcce1c2923688302fab98f037a5087\n" +
                                               "20971abd43902c86e1c841f557f4d65ca878fc5949619992437e5f949209\n" +
                                               "8d03aaccbcdefd108f1d0a52043d58d62ed6b117efa59135892acdb8555b\n" +
                                               "8ceb2ef104f884b1e9eeacba1db8a7fe493ce86e4e57fc3a76313b27f0f0\n" +
                                               "743ceef3f40eccdeedabca0d73a3144ab73312bfc7db156a6edaf8c7cc3d\n" +
                                               "e77148ffcae65e9cd5b8d5a6598f798bb8dd4144250796d05430fad1d176\n" +
                                               "2fc1cabe373bca479f781481f063012a14f693af9f1db9e19cf416914724\n" +
                                               "a53519744e66e3fc02575b949f1d182ee828e5a38aa2809807959f650bfe\n" +
                                               "963e5a86c5652ee231571f13a9636ece1fb99f98cd7cfeba11c70c9f8380\n" +
                                               "99bb541eefc460516185a46963cfc399609837eacedf2b58ebc22eeda808\n" +
                                               "7d06423fc083a48e100b756d111cc1c109c2d274479cec84c5159d434b58\n" +
                                               "7e2dad8fd7439228c5f484d1a1f80c86cfc1205d58be50a43b2a3d828d79\n" +
                                               "faa939ab47383dc54b76adcbb3536051894e8f88918f3dc1f5350ccf5008\n" +
                                               "60a615116def64c0950301baab844f85778c7486d9fe4f341b70c6a7176e\n" +
                                               "6aa00f2e25751cd07a5b8b4d7f4b108c303af48de3fbc695c472f0360dec\n" +
                                               "75aa2ecce3f0f76db4380039927f872dc70c1aa86c37af93592cfc71f65b\n" +
                                               "2404893c8ed8f3f3b701c1d7a0386257ba3d5421e32dd5965fa9dd297dde\n" +
                                               "560f1afd2574f88e2f726e6b5e2f0e9c8ebf9e3182aa89401600aa6926de\n" +
                                               "c4d1a32db0429b9214f16c25300af92bfc35e17c4b70251e82f4dbfff643\n" +
                                               "16987e8d3cf2c2e45b9ecf76984dfbc819b96347f9c595257bbe32e83947\n" +
                                               "e72e87f1e70ebabecd0eff8116878d06a17bd903074643b1ea62fe003519\n" +
                                               "a222d4d1db9d239f8084e1919f9c90a0146001c047df192899f59ff9d506\n" +
                                               "c2ad0e9cf7afe3bbe16be365e3487dffc270f7c87a8a55be04c85050bf33\n" +
                                               "eb32ad47a7a43500b6b082cbbe6156ec2bc82e5a5dce";



    private CodenvyLicenseManager codenvyLicenseManager;
    private CodenvyLicenseFactory codenvyLicenseFactory;


    @BeforeMethod
    public void setUp() {
        codenvyLicenseManager = INJECTOR.getInstance(CodenvyLicenseManager.class);
        codenvyLicenseFactory = INJECTOR.getInstance(CodenvyLicenseFactory.class);
    }

    @Test
    public void testStoreLoadLicense() throws Exception {
        codenvyLicenseManager.store(codenvyLicenseFactory.create(LICENCE_TEXT));

        assertEquals(codenvyLicenseManager.load().getLicenseText(), LICENCE_TEXT);
    }

    @Test(expectedExceptions = InvalidLicenseException.class)
    public void testValidateShouldThrowExceptionIfLicenseInvalid() throws Exception {
        codenvyLicenseManager.store(codenvyLicenseFactory.create(INVALID_LICENCE_TEXT));
    }

    @Test(expectedExceptions = LicenseNotFoundException.class)
    public void testDeleteLicense() throws Exception {
        codenvyLicenseManager.store(codenvyLicenseFactory.create(LICENCE_TEXT));

        codenvyLicenseManager.delete();

        codenvyLicenseManager.load();
    }
}
