/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.jose.jws;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.utils.crypto.HmacUtils;

public class HmacJwsSignatureVerifier implements JwsSignatureVerifier {
    private byte[] key;
    private AlgorithmParameterSpec hmacSpec;
    
    public HmacJwsSignatureVerifier(byte[] key) {
        this(key, null);
    }
    public HmacJwsSignatureVerifier(byte[] key, AlgorithmParameterSpec spec) {
        this.key = key;
        this.hmacSpec = spec;
    }
    public HmacJwsSignatureVerifier(String encodedKey) {
        try {
            this.key = Base64UrlUtility.decode(encodedKey);
        } catch (Base64Exception ex) {
            throw new SecurityException();
        }
    }
    
    @Override
    public boolean verify(JwtHeaders headers, String unsignedText, byte[] signature) {
        byte[] expected = computeMac(headers, unsignedText);
        return Arrays.equals(expected, signature);
    }
    
    private byte[] computeMac(JwtHeaders headers, String text) {
        return HmacUtils.computeHmac(key, 
                                     Algorithm.toJavaName(headers.getAlgorithm()),
                                     hmacSpec,
                                     text);
    }
    
}