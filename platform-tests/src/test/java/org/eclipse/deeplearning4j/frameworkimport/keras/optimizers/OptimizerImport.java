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

package org.eclipse.deeplearning4j.frameworkimport.keras.optimizers;

import org.deeplearning4j.BaseDL4JTest;
import org.deeplearning4j.frameworkimport.keras.keras.KerasModel;
import org.deeplearning4j.frameworkimport.keras.keras.KerasSequentialModel;
import org.deeplearning4j.frameworkimport.keras.keras.utils.KerasModelBuilder;
import org.deeplearning4j.common.util.ND4JFileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nd4j.common.resources.Resources;
import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
@Tag(TagNames.FILE_IO)
@Tag(TagNames.KERAS)
@NativeTag
public class OptimizerImport extends BaseDL4JTest {

    @Test
    public void importAdam() throws Exception {
        importSequential("modelimport/keras/optimizers/adam.h5");
    }

    @Test
    public void importAdaMax() throws Exception {
        importSequential("modelimport/keras/optimizers/adamax.h5");
    }

    @Test
    public void importAdaGrad() throws Exception {
        importSequential("modelimport/keras/optimizers/adagrad.h5");
    }

    @Test
    public void importAdaDelta() throws Exception {
        importSequential("modelimport/keras/optimizers/adadelta.h5");
    }

    @Test
    public void importSGD() throws Exception {
        importSequential("modelimport/keras/optimizers/sgd.h5");
    }

    @Test
    public void importRmsProp() throws Exception {
        importSequential("modelimport/keras/optimizers/rmsprop.h5");
    }

    @Test
    public void importNadam() throws Exception {
        importSequential("modelimport/keras/optimizers/nadam.h5");
    }

    private void importSequential(String modelPath) throws Exception {
        File modelFile = ND4JFileUtils.createTempFile("tempModel", ".h5");
        try(InputStream is = Resources.asStream(modelPath)) {
            Files.copy(is, modelFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            KerasModelBuilder builder = new KerasModel().modelBuilder().modelHdf5Filename(modelFile.getAbsolutePath())
                    .enforceTrainingConfig(false);

            KerasSequentialModel model = builder.buildSequential();
            model.getMultiLayerNetwork();
        }
    }
}
