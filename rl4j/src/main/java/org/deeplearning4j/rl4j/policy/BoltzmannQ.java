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

package org.deeplearning4j.rl4j.policy;

import org.deeplearning4j.rl4j.environment.action.Action;
import org.deeplearning4j.rl4j.environment.observation.Observation;
import org.deeplearning4j.rl4j.environment.action.space.ActionSpace;
import org.deeplearning4j.rl4j.network.CommonOutputNames;
import org.deeplearning4j.rl4j.network.OutputNeuralNet;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.rng.Random;

import lombok.Builder;
import lombok.NonNull;

import static org.nd4j.linalg.ops.transforms.Transforms.exp;

public class BoltzmannQ<OBSERVATION extends Observation, ACTION extends Action> extends BasePolicy<OBSERVATION,ACTION> {

    final private Random rnd;

    @Builder
    public BoltzmannQ(@NonNull OutputNeuralNet neuralNet, @NonNull ActionSpace<ACTION> actionSpace, Random random) {
    	super(neuralNet,actionSpace);
        this.rnd = random;
    }

    @SuppressWarnings("unchecked")
	@Override
    public ACTION nextAction(OBSERVATION obs) {
        INDArray output = neuralNet.output(obs).get(CommonOutputNames.QValues);
        INDArray exp = exp(output);

        double sum = exp.sum(1).getDouble(0);
        double picked = rnd.nextDouble() * sum;
        for (int i = 0; i < exp.columns(); i++) {
            if (picked < exp.getDouble(i))
                return  (ACTION) actionSpace.fromInteger(i);
        }
        return (ACTION) actionSpace.fromInteger(-1);
    }
}
