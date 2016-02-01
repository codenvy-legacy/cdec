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
package com.codenvy.im.managers;

import com.codenvy.im.exceptions.InvalidLicenseException;
import com.codenvy.im.exceptions.LicenseNotFoundException;
import com.codenvy.im.utils.InjectorBootstrap;

import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class CodenvyLicenseManagerTest {

    private static final String INVALID_LICENCE_TEXT = "some license";

    // Custom Signed Features: | license-type = product key | expiration-date = 2020/12/01 | users = 10 |
    private static final String LICENCE_TEXT = "1c4594dd37a0a02e8522c0c7670431dcce1c2923688302fac9bacc729397\n" +
                                               "ae3d6aa5f250961d47b63e5a0870820742dffd4a3e1f5fd9fcbf496a5be9\n" +
                                               "c102a2c5f188820f6c3efd48f092bed1d8078b4821fbbc983d2ebbd23fd0\n" +
                                               "f80a7292d96eccc8812d8c08efc9834718b516a014f3dce47be5bdef54ee\n" +
                                               "2a350d0d3f1df4017144e55d36e5704bac15220f1662718b55848329aced\n" +
                                               "6d834efc998ac6713aa2ee02e10ff9775e525d4585dba741ec453816df32\n" +
                                               "2cc9741e4b667f02aff1c28345506e4e03c6a6b98fc41dfca454bf961cd0\n" +
                                               "2ad22e05b8bf7bfc291db7c85aa5b3757d1920603bfbe7c050b9630ac774\n" +
                                               "950c89d4b5938181ca5bd565c41ea84b217c7e8f32fb72e2614dbba22f4a\n" +
                                               "7d679a684d19343eab0e4c5e5f8d1d0997dd558bb575c19da9ddfc2910c1\n" +
                                               "4e9db2adeff1cae842d85abc7968f19bcf8019a8743cf9ce60cbc4b80302\n" +
                                               "7a6829b0b4f6ca54ab22476777d13a7bce251c1e3c61b21a556da8a7c245\n" +
                                               "ad1859a7b3e71657c0e619e6a927ba65593e7d1180b51a6107d263755c5c\n" +
                                               "d189a8fe1dbe30d2e61f070a94ad4b0a0e0738b660aca5478f1589c582de\n" +
                                               "99fd9909e089f8ddf9c0136d99f2045b44000f5aa3a46ba3c35b2d6886c0\n" +
                                               "b7d9f35da3289f3dc8a068c3dfa59dc75307beb40c26e13d73cb02a9ecb2\n" +
                                               "f963c3b03e81bd382b9e1b2ad4e6bc23d7eb642b0c39c8a465e085661ac7\n" +
                                               "cf5cfb23d9f96912b09ed85def6f362e0ff83688b263961af78a8b081d84\n" +
                                               "95865d428129c7a98dbd67730df5c8eeb34d110b333d9793d91994c42f76\n" +
                                               "17fc2e2f0a3abe8aedf15fe9e20dc156b1b012090db79bd9d8abf9779e38\n" +
                                               "1b517020d87a3471cb16c6c21b449bc1bde163661e51877c82e3d48a81bb\n" +
                                               "4db8efe6257256f199543a9b5f1c705113e48de50dbe90d3e6b039a9db0d\n" +
                                               "a5f00cf49bf7adcb63b4a155c7ce8f0cbdad715e62382008f6d392404696\n" +
                                               "1ff59ce9582b8486d856d7262f07d4164410070ecd63d21598aa40b9e3c8\n" +
                                               "d6f1a737dc18ba44b97ebc3a5cd4af64";

    // Custom Signed Features: | license-type = product key | expiration-date = 2012/10/01 | users = 10 |
    private static final String LICENSE_TEXT_EXPIRED_INCOMPLETE = "1c4594dd37a0a02e8522c0c7670431dcce1c2923688302fac9bacc729397\n" +
                                                                  "ae3dc7eaf98ed5f5aa037e11ff53d9d1f396a32874c6ac16ff2e413c7f1e\n" +
                                                                  "e0f6d0ac841b83490b93820f5b39666539e143d21a142620012937a7f8d1\n" +
                                                                  "187801ede49b7c063ae0ba66a733e03d4a2c94c1997d3f299bb0d594a687\n" +
                                                                  "a216bba8d9d41e53725026a9a78c86e0136af1c5217df75676efa447146e\n" +
                                                                  "8fbe09c383be242309c0b74d98936dc6ef5665e4f8ad879c84eccbbd5ad8\n" +
                                                                  "0f63ffdda76bba512a2758a5f03d81184f29a61c2741847e5a31eff86b10\n" +
                                                                  "7029b9d366c019bdb904613669b5bf618d3c141f51deae00a709166889b1\n" +
                                                                  "2e2c6c141a826873b92e1aa616f1ebbf418f45a560d335247b9a78d538c0\n" +
                                                                  "c4d197abe86ceecb437fa76719f8f79290435756d7e37e387e9b1da3456f\n" +
                                                                  "86fd7e51935100f2aaf88c2a3280a25a97e5ac31fae25abac8bd47845814\n" +
                                                                  "94fbc0fb771a16f5496c89a1b2c0ef9dd885a6dc1af4af4469225e09c57a\n" +
                                                                  "aaded87595355f361f63ed195176b412c7758041bd550134c7dd20624d57\n" +
                                                                  "9b218f1cb2f3dc61f9c8303cb1f61fe18486ee4c1e50a5fc0021705994cb\n" +
                                                                  "103c1c08e74e6af268b45fd4d7130b88ddc26e782e150b999f7415b3c5fd\n" +
                                                                  "ed6404b74ca4b0c2080165101020347af5101e17fd847f8773833c12863f\n" +
                                                                  "0ed804f7bee0c25c46e07a7c6bd63f346e398b1da404bafb1c8a27dd8ad7\n" +
                                                                  "c9f61a332ae80de3a7ef40835fb048d8b6d9154bad95cbc31ee76b483c22\n" +
                                                                  "d51aa04341d38db9bde3c04957f6d182c31391aa79518e45a13b28ff39b8\n" +
                                                                  "30e1dca09e4a6b67ca2b95afcf73062dc088904a72a32c0650e4cdedc4e7\n" +
                                                                  "da3c45e9b32fa716f77bd648db2cbfe922800c847edf735c21f946b7a05d\n" +
                                                                  "9441b02be6c9134cbc4d5fe6edbe90f0bfd906907026616926a45c4eda69\n" +
                                                                  "5729abb22921ac20e8d7172b6089bbf3b291cafb05c4cd75e6101be5ff32\n" +
                                                                  "bea45a41570d033912427b14a483699d85926cdf70546a3d881e02eb4b3f\n" +
                                                                  "dbb264f14ce89c582777a319dbd2c1e7";

    private CodenvyLicenseManager codenvyLicenseManager;


    @BeforeMethod
    public void setUp() {
        codenvyLicenseManager = InjectorBootstrap.INJECTOR.getInstance(CodenvyLicenseManager.class);
    }

    @Test
    public void testStoreLoadLicense() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        assertEquals(codenvyLicenseManager.load(), LICENCE_TEXT);
    }

    @Test
    public void testValidate() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        codenvyLicenseManager.validate();
    }

    @Test(expectedExceptions = InvalidLicenseException.class)
    public void testValidateShouldThrowExceptionIfLicenseInvalid() throws Exception {
        codenvyLicenseManager.store(INVALID_LICENCE_TEXT);
    }

    @Test
    public void testGetNumberOfUsers() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        assertEquals(codenvyLicenseManager.getNumberOfUsers(), 10);
    }

    @Test
    public void testLicenseType() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        assertEquals(codenvyLicenseManager.getLicenseType(), CodenvyLicenseManager.LicenseType.PRODUCT_KEY);
    }

    @Test
    public void testGetExpirationDate() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(codenvyLicenseManager.getExpirationDate());

        assertEquals(calendar.get(Calendar.YEAR), 2020);
        assertEquals(calendar.get(Calendar.MONTH), Calendar.DECEMBER);
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), 1);
    }

    @Test
    public void testCheckExpirationDateShouldReturnFalse() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        assertFalse(codenvyLicenseManager.isLicenseExpired());
    }

    @Test
    public void testCheckExpirationDateShouldReturnTrueLicenseExpired() throws Exception {
        codenvyLicenseManager.store(LICENSE_TEXT_EXPIRED_INCOMPLETE);

        assertTrue(codenvyLicenseManager.isLicenseExpired());
    }

    @Test
    public void testGetCustomFeatures() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        Map<CodenvyLicenseManager.LicenseFeature, String> customFeatures = codenvyLicenseManager.getCustomFeatures();
        assertEquals(customFeatures.size(), 3);

        for (CodenvyLicenseManager.LicenseFeature feature : CodenvyLicenseManager.LicenseFeature.values()) {
            assertTrue(customFeatures.containsKey(feature));
        }
    }

    @Test(expectedExceptions = LicenseNotFoundException.class)
    public void testDeleteLicense() throws Exception {
        codenvyLicenseManager.store(LICENCE_TEXT);

        codenvyLicenseManager.delete();

        codenvyLicenseManager.load();
    }
}
