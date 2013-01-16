/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.huahinframework.manager.rest.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wink.common.internal.utils.MediaTypeUtils;
import org.apache.wink.common.model.multipart.InMultiPart;
import org.huahinframework.manager.response.Response;
import org.json.JSONObject;

/**
 *
 */
@Path("/hive")
public class HiveService extends Service {
    private static final Log log = LogFactory.getLog(HiveService.class);

    private static final String DRIVER_NAME = "org.apache.hadoop.hive.jdbc.HiveDriver";
    private static final String CONNECTION_FORMAT = "jdbc:hive://%s/default";

    private static final String JSON_QUERY = "query";

    private String hiveserver;


    @Path("/execute")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaTypeUtils.MULTIPART_FORM_DATA)
    public JSONObject execute(InMultiPart inMP) {
        Map<String, String> status = new HashMap<String, String>();
        status.put(Response.STATUS, "SCCESS");
        try {
            if (!inMP.hasNext()) {
                status.put(Response.STATUS, "Query is empty");
                return new JSONObject(status);
            }

            JSONObject argument = createJSON(inMP.next().getInputStream());
            String query = argument.getString(JSON_QUERY);
            if (query == null || query.isEmpty()) {
                status.put(Response.STATUS, "Query is empty");
                return new JSONObject(status);
            }

            Class.forName(DRIVER_NAME);
            Connection con = DriverManager.getConnection(String.format(CONNECTION_FORMAT, hiveserver), "", "");
            Statement stmt = con.createStatement();

            stmt.execute(query);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            status.put(Response.STATUS, e.getMessage());
        }

        return new JSONObject(status);
    }

    @Path("/executeQuery")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaTypeUtils.MULTIPART_FORM_DATA)
    public void executeQuery(@Context HttpServletResponse response,
                             InMultiPart inMP) throws IOException {
        OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream());
        Map<String, String> status = new HashMap<String, String>();
        try {
            if (!inMP.hasNext()) {
                throw new RuntimeException("Query is empty");
            }

            JSONObject argument = createJSON(inMP.next().getInputStream());
            String query = argument.getString(JSON_QUERY);
            if (query == null || query.isEmpty()) {
                status.put(Response.STATUS, "Query is empty");
                JSONObject jsonObject = new JSONObject(status);
                out.write(jsonObject.toString());
                out.flush();
                out.close();
                return;
            }

            Class.forName(DRIVER_NAME);
            Connection con = DriverManager.getConnection(String.format(CONNECTION_FORMAT, hiveserver), "", "");
            Statement stmt = con.createStatement();

            ResultSet resultSet = stmt.executeQuery(query);
            while (resultSet.next()) {
                JSONObject jsonObject = new JSONObject();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    jsonObject.put(resultSet.getMetaData().getColumnName(i), resultSet.getString(i));
                }
                out.write(jsonObject.toString());
                out.flush();
            }
            con.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            status.put(Response.STATUS, e.getMessage());
            JSONObject jsonObject = new JSONObject(status);
            out.write(jsonObject.toString());
            out.flush();
            out.close();
        }
    }

    /**
     *
     */
    public void init() {
        hiveserver = properties.getHiveserver();
    }
}