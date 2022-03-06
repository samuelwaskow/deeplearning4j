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

package org.deeplearning4j.rl4j.agent.learning.algorithm.dqn;

import lombok.NonNull;
import org.deeplearning4j.rl4j.agent.learning.update.Features;
import org.deeplearning4j.rl4j.environment.action.DiscreteAction;
import org.deeplearning4j.rl4j.environment.action.space.ActionSpace;
import org.deeplearning4j.rl4j.environment.observation.Observation;
import org.deeplearning4j.rl4j.network.CommonOutputNames;
import org.deeplearning4j.rl4j.network.OutputNeuralNet;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * The base of all BaseDQN based algorithms
 *
 * @author Alexandre Boulanger
 *
 */
public abstract class BaseDQNAlgorithm<OBSERVATION extends Observation, ACTION extends DiscreteAction> extends BaseTransitionTDAlgorithm<OBSERVATION,ACTION> {

    private final OutputNeuralNet targetQNetwork;

    /**
     * In literature, this corresponds to Q<sub>net</sub>(s(t+1), a)
     */
    protected INDArray qNetworkNextFeatures;

    /**
     * In literature, this corresponds to Q<sub>tnet</sub>(s(t+1), a)
     */
    protected INDArray targetQNetworkNextFeatures;

    protected BaseDQNAlgorithm(OutputNeuralNet qNetwork,
                               @NonNull OutputNeuralNet targetQNetwork,
                               @NonNull ActionSpace<ACTION> actionSpace,
                               BaseTransitionTDAlgorithm.Configuration configuration) {
        super(qNetwork, actionSpace, configuration);
        this.targetQNetwork = targetQNetwork;
    }

    @Override
    protected void initComputation(Features features, Features nextFeatures) {
        super.initComputation(features, nextFeatures);

        qNetworkNextFeatures = qNetwork.output(nextFeatures).get(CommonOutputNames.QValues);
        targetQNetworkNextFeatures = targetQNetwork.output(nextFeatures).get(CommonOutputNames.QValues);
    }
}
