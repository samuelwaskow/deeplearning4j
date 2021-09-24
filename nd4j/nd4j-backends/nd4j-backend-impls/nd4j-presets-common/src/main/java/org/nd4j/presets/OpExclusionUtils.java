/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.nd4j.presets;

import org.bytedeco.javacpp.tools.Info;
import org.bytedeco.javacpp.tools.InfoMap;
import org.bytedeco.javacpp.tools.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Common op exclusion utility for nd4j backend presets
 * that leverage the libnd4j codebase.
 *
 * @author Adam Gibson
 */
public class OpExclusionUtils {

    /**
     * Prints the ops discovered in the libnd4j code base,
     * adds proper exclusions based on the include_ops.h
     * file generated by libnd4j.
     * see also:
     * https://github.com/eclipse/deeplearning4j/blob/master/libnd4j/blas/CMakeLists.txt#L76
     * https://github.com/eclipse/deeplearning4j/blob/master/libnd4j/buildnativeoperations.sh#L517
     *
     * All ops will either be all included (SD_OPS is 1/true)
     * when building the libnd4j code base + headers
     * or it will include a list of ops included from the command line.
     *
     * This function handles evaluating what ops are available
     * and what ops will be included in the intended
     * nd4j backend binary.
     *
     * @param logger the javacpp logger to use to log information
     * @param properties the javacpp properties at runtime
     * @param infoMap the info map to use when adding proper definitions
     *                for headers based on the nd4j backend loaded.
     */
    public static void processOps(Logger logger, java.util.Properties properties, InfoMap infoMap) {
        // pick up custom operations automatically from CustomOperations.h and headers in libnd4j
        String separator = properties.getProperty("platform.path.separator");
        String[] includePaths = properties.getProperty("platform.includepath").split(separator);
        File file = null;
        File opFile = null;
        boolean foundCustom = false;
        boolean foundOps = false;
        for (String path : includePaths) {
            if(!foundCustom) {
                file = new File(path, "ops/declarable/CustomOperations.h");
                if (file.exists()) {
                    foundCustom = true;
                }
            }

            if(!foundOps) {
                opFile = new File(path, "generated/include_ops.h");
                if (opFile.exists()) {
                    foundOps = true;
                }
            }

            if(foundCustom && foundOps) {
                break;
            }
        }


        boolean allOps = false;
        Set<String> opsToExclude = new HashSet<>();
        try (Scanner scanner = new Scanner(opFile, "UTF-8")) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if(line.contains("SD_ALL_OPS")) {
                    allOps = true;
                    System.out.println("All ops found.");
                    break;
                }

                String[] lineSplit = line.split(" ");
                if(lineSplit.length < 2) {
                    System.err.println("Unable to add op to exclude. Invalid op found: " + line);
                } else {
                    String opName = lineSplit[1].replace("OP_","");
                    opsToExclude.add(opName);
                    //usually gradient ops are co located in the same block
                    opsToExclude.add(opName + "_bp");
                }

            }
        } catch (IOException e) {
            throw new RuntimeException("Could not parse CustomOperations.h and headers", e);
        }


        List<File> files = new ArrayList<>();
        List<String> opTemplates = new ArrayList<>();
        if(file == null) {
            throw new IllegalStateException("No file found in include paths. Please ensure one of the include paths leads to path/ops/declarable/CustomOperations.h");
        }
        files.add(file);
        File[] headers = new File(file.getParent(), "headers").listFiles();
        if(headers == null) {
            throw new IllegalStateException("No headers found for file " + file.getAbsolutePath());
        }

        files.addAll(Arrays.asList(headers));
        Collections.sort(files);

        for (File f : files) {
            try (Scanner scanner = new Scanner(f, "UTF-8")) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.startsWith("DECLARE_")) {
                        try {
                            int start = line.indexOf('(') + 1;
                            int end = line.indexOf(',');
                            if (end < start) {
                                end = line.indexOf(')');
                            }
                            String name = line.substring(start, end).trim();
                            opTemplates.add(name);
                        } catch(Exception e) {
                            throw new RuntimeException("Could not parse line from CustomOperations.h and headers: \"" + line + "\"", e);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not parse CustomOperations.h and headers", e);
            }
        }

        Collections.sort(opTemplates);
        logger.info("Ops found in CustomOperations.h and headers: " + opTemplates);
        //we will be excluding some ops based on the ops defined in the generated op inclusion file
        if(!allOps) {
            logger.info("Found ops to only include " + opsToExclude);
            for(String op : opTemplates)
                if(!opsToExclude.contains(op)) {
                    logger.info("Excluding op " + op);
                    infoMap.put(new Info("NOT_EXCLUDED(OP_" + op + ")")
                            .skip(true)
                            .define(false));
                } else {
                    logger.info("Including " + op);
                    infoMap.put(new Info("NOT_EXCLUDED(OP_" + op + ")").define(true));
                    infoMap.put(new Info("NOT_EXCLUDED(OP_" + op + "_bp)").define(true));

                }
        }
    }
}
