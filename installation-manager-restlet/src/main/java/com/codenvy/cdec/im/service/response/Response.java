/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.cdec.im.service.response;

import java.util.Arrays;
import java.util.List;


/** @author Dmytro Nochevnov */
// TODO check getjson?
public class Response {
    Status status;
    
    List<ArtifactInfo> artifacts;
    
    public Response(Status status) {
        this.status = status;
    }

    public Response(Status status, List<ArtifactInfo> artifacts) {
        this.artifacts = artifacts;
        this.status = status;        
    }
    
    public Response(Status status, ArtifactInfo artifactInfo) {
        this(status, Arrays.asList(new ArtifactInfo[]{ artifactInfo }));
    }

    public Status getStatus() {
        return status;
    }

    public List<ArtifactInfo> getArtifacts() {
        return artifacts;
    }
    
    public static class Status {
        private StatusCode code;        
        private String message;
                
        public Status(StatusCode code, String message) {
            this.code = code;
            this.message = message;
        }

        public Status(StatusCode code) {
            this(code, null);
        }

        public StatusCode getStatusCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

    }
}
