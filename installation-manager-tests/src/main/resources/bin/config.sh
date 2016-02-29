#!/bin/bash
#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2015] Codenvy, S.A.
# All Rights Reserved.
# NOTICE: All information contained herein is, and remains
# the property of Codenvy S.A. and its suppliers,
# if any. The intellectual and technical concepts contained
# herein are proprietary to Codenvy S.A.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Codenvy S.A..
#

UPDATE_SERVER="http://updater-nightly.codenvy-dev.com"
UPDATE_SERVICE="${UPDATE_SERVER}/update"
SAAS_SERVER="http://nightly4.codenvy-stg.com"
TEST_LOG="installation-manager-test.log"
HOST_URL="codenvy"
NEW_HOST_URL="test.codenvy"

AVAILABLE_CODENVY_VERSIONS=$(curl -s -X GET ${UPDATE_SERVICE}/repository/updates/codenvy)

PREV_CODENVY3_VERSION=$(echo ${AVAILABLE_CODENVY_VERSIONS} | sed 's/.*"\(3[^"]*\)","3[^"]*".*\]/\1/')
LATEST_CODENVY3_VERSION=$(echo ${AVAILABLE_CODENVY_VERSIONS} | sed 's/.*"\(3[^"]*\)".*/\1/')

PREV_CODENVY4_VERSION=$(echo ${AVAILABLE_CODENVY_VERSIONS} | sed 's/.*"\(4[^"]*\)","4[^"]*".*\]/\1/')
LATEST_CODENVY4_VERSION=$(echo ${AVAILABLE_CODENVY_VERSIONS} | sed 's/.*"\(4[^"]*\)".*/\1/')

LATEST_STABLE_CODENVY_VERSION=$(echo ${AVAILABLE_CODENVY_VERSIONS} | sed 's/.*"\([^"]*[0-9]\)".*\]/\1/')

AVAILABLE_IM_CLI_CLIENT_VERSIONS=$(curl -s -X GET ${UPDATE_SERVICE}/repository/updates/installation-manager-cli)
PREV_IM_CLI_CLIENT_VERSION=`echo ${AVAILABLE_IM_CLI_CLIENT_VERSIONS} | sed 's/.*"\([^"]*\)","[^"]*"\]/\1/'`
LATEST_IM_CLI_CLIENT_VERSION=`echo ${AVAILABLE_IM_CLI_CLIENT_VERSIONS} | sed 's/.*"\([^"]*\)".*/\1/'`

SINGLE_NODE_VAGRANT_FILE="../vagrant/single/CentOS7/Vagrantfile"
MULTI_NODE_VAGRANT_FILE="../vagrant/multi/CentOS7/Vagrantfile"
MULTI_NODE_WITH_ADDITIONAL_NODES_VAGRANT_FILE="../vagrant/multi-with-additional-nodes/CentOS7/Vagrantfile"
SINGLE_CODENVY4_WITH_ADDITIONAL_NODES_VAGRANT_FILE="../vagrant/single-codenvy4-with-additional-nodes/CentOS7/Vagrantfile"

CODENVY_LICENSE_PRODUCT_ID=testId
CODENVY_LICENSE_PUBLIC_KEY=30820122300d06092a864886f70d01010105000382010f00303032301006072a8648ce3d02002EC311215SHA512withECDSA106052b81040006031e0004cd5355181ff629780e4564321e773f8b38a6e7a8bbeb4262c8f0221fG82010a0282010100a455c6416348b0a6fe5b61af2d0c1dc7ec1afb022a9831d1f0c90698b7078205d54ac8b43aece203f1f9524618f6ae7f2fa563e7d15d37c772ef88954e96d7f65fa3455a27d360005f5d2c4db9777caabfca722d0c1c86a2c175626703RSA4204813SHA512withRSA3ea2263b55d338d58b30901e6bf52455d850217f602c0fce9e77de93faf345ac6d6bb4fe9026330abe7524447d04c6d79a3fe9e9b25d9dcdb14717e45b78b1a0cfb40bcf54af4f257ac3ac506042758e476bbfbf459e1544e099cfacfe12501e7b1c03a9999e719825b4b4624086ad908eb2cbcbd8263242071958276f7c43cec3dd4121ea3bc2af6532fc2f1b28fbdd5579f8cfbd30451b52b679c068f3d52a1571d4290203010001
CODENVY_LICENSE=1c4594dd37a0a02e8522c0c7670431dcce1c2923688302fac9bacc729397ae3d6aa5f250961d47b63e5a0870820742dffd4a3e1f5fd9fcbf496a5be9c102a2c5f188820f6c3efd48f092bed1d8078b4821fbbc983d2ebbd23fd0f80a7292d96eccc8812d8c08efc9834718b516a014f3dce47be5bdef54ee2a350d0d3f1df4017144e55d36e5704bac15220f1662718b55848329aced6d834efc998ac6713aa2ee02e10ff9775e525d4585dba741ec453816df322cc9741e4b667f02aff1c28345506e4e03c6a6b98fc41dfca454bf961cd02ad22e05b8bf7bfc291db7c85aa5b3757d1920603bfbe7c050b9630ac774950c89d4b5938181ca5bd565c41ea84b217c7e8f32fb72e2614dbba22f4a7d679a684d19343eab0e4c5e5f8d1d0997dd558bb575c19da9ddfc2910c14e9db2adeff1cae842d85abc7968f19bcf8019a8743cf9ce60cbc4b803027a6829b0b4f6ca54ab22476777d13a7bce251c1e3c61b21a556da8a7c245ad1859a7b3e71657c0e619e6a927ba65593e7d1180b51a6107d263755c5cd189a8fe1dbe30d2e61f070a94ad4b0a0e0738b660aca5478f1589c582de99fd9909e089f8ddf9c0136d99f2045b44000f5aa3a46ba3c35b2d6886c0b7d9f35da3289f3dc8a068c3dfa59dc75307beb40c26e13d73cb02a9ecb2f963c3b03e81bd382b9e1b2ad4e6bc23d7eb642b0c39c8a465e085661ac7cf5cfb23d9f96912b09ed85def6f362e0ff83688b263961af78a8b081d8495865d428129c7a98dbd67730df5c8eeb34d110b333d9793d91994c42f7617fc2e2f0a3abe8aedf15fe9e20dc156b1b012090db79bd9d8abf9779e381b517020d87a3471cb16c6c21b449bc1bde163661e51877c82e3d48a81bb4db8efe6257256f199543a9b5f1c705113e48de50dbe90d3e6b039a9db0da5f00cf49bf7adcb63b4a155c7ce8f0cbdad715e62382008f6d3924046961ff59ce9582b8486d856d7262f07d4164410070ecd63d21598aa40b9e3c8d6f1a737dc18ba44b97ebc3a5cd4af64


STORAGE_DIR=/var/lib/codenvy/im/storage
STORAGE_FILE=/var/lib/codenvy/im/storage/config.properties