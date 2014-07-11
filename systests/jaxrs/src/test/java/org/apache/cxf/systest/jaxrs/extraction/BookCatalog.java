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

package org.apache.cxf.systest.jaxrs.extraction;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.search.lucene.LuceneQueryVisitor;
import org.apache.cxf.jaxrs.ext.search.tika.LuceneDocumentMetadata;
import org.apache.cxf.jaxrs.ext.search.tika.TikaLuceneContentExtractor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.parser.pdf.PDFParser;

@Path("/catalog")
public class BookCatalog {
    private final TikaLuceneContentExtractor extractor = new TikaLuceneContentExtractor(new PDFParser());    
    private final Directory directory = new RAMDirectory();
    private final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
    private final IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    
    @POST
    @Consumes("multipart/form-data")
    public Response addBook(final MultipartBody body) throws Exception {
        for (final Attachment attachment: body.getAllAttachments()) {
            final DataHandler handler =  attachment.getDataHandler();
            
            if (handler != null) {
                final String source = handler.getName();                
                final LuceneDocumentMetadata metadata = new LuceneDocumentMetadata()
                    .withSource(source)
                    .withField("modified", Date.class);
                
                final Document document = extractor.extract(handler.getInputStream(), metadata);
                if (document != null) {                    
                    final IndexWriter writer = new IndexWriter(directory, config);
                    
                    try {
                        writer.addDocument(document);
                        writer.commit();
                    } finally {
                        writer.close();
                    }
                }
            }
        }        
        
        return Response.ok().build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<ScoreDoc> findBook(@Context SearchContext searchContext) throws IOException {
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);        

        try {
            final Map< String, Class< ? > > fieldTypes = new HashMap< String, Class< ? > >();
            fieldTypes.put("modified", Date.class);
            
            LuceneQueryVisitor<SearchBean> visitor = new LuceneQueryVisitor<SearchBean>("ct", "contents");
            visitor.setPrimitiveFieldTypeMap(fieldTypes);
            visitor.visit(searchContext.getCondition(SearchBean.class));
    
            return Arrays.asList(searcher.search(visitor.getQuery(), null, 1000).scoreDocs);
        } finally {
            reader.close();
        }
    }
}

