/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.api.ApiConfiguration;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.Quantity;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.mainnet.ImmutableTransactionValidationParams;
import org.hyperledger.besu.ethereum.mainnet.TransactionValidationParams;
import org.hyperledger.besu.ethereum.transaction.CallParameter;
import org.hyperledger.besu.ethereum.transaction.TransactionSimulator;
import org.hyperledger.besu.ethereum.transaction.TransactionSimulatorResult;
import org.hyperledger.besu.evm.tracing.OperationTracer;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public class EthGasPrice implements JsonRpcMethod {

  private final BlockchainQueries blockchainQueries;
  private final ApiConfiguration apiConfiguration;
  private final TransactionSimulator transactionSimulator;

  private static final Bytes BYTES_ZERO =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  private static final Bytes STATUS_SIGNATURE =
      Hash.keccak256(Bytes.of("status()".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes GASPRICE_SIGNATURE =
      Hash.keccak256(Bytes.of("gasPrice()".getBytes(UTF_8))).slice(0, 4);

  public EthGasPrice(
      final BlockchainQueries blockchainQueries,
      final ApiConfiguration apiConfiguration,
      final TransactionSimulator transactionSimulator) {
    this.blockchainQueries = blockchainQueries;
    this.apiConfiguration = apiConfiguration;
    this.transactionSimulator = transactionSimulator;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GAS_PRICE.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(), Quantity.create(calculateGasPrice()));
  }

  private Wei calculateGasPrice() {
    final Wei defaultGasPrice = blockchainQueries.gasPrice();
    // Simulate "status" function call
    final Bytes statusValue = callRemoteGasPrice(STATUS_SIGNATURE);
    // If status is valid and non-zero, simulate "gasPrice" function call
    if (!statusValue.isZero()) {
      final Bytes gasPriceValue = callRemoteGasPrice(GASPRICE_SIGNATURE);

      // Return gas price from simulation if present
      if (!gasPriceValue.isZero()) {
        return Wei.wrap(gasPriceValue);
      }
    }
    return isGasPriceLimitingEnabled() ? limitGasPrice(defaultGasPrice) : defaultGasPrice;
  }

  private boolean isGasPriceLimitingEnabled() {
    return apiConfiguration.isGasAndPriorityFeeLimitingEnabled();
  }

  private Wei limitGasPrice(final Wei gasPrice) {
    final Wei lowerBoundGasPrice = blockchainQueries.gasPriceLowerBound();
    final Wei forcedLowerBound =
        calculateBound(
            lowerBoundGasPrice, apiConfiguration.getLowerBoundGasAndPriorityFeeCoefficient());
    final Wei forcedUpperBound =
        calculateBound(
            lowerBoundGasPrice, apiConfiguration.getUpperBoundGasAndPriorityFeeCoefficient());

    return gasPrice.compareTo(forcedLowerBound) <= 0
        ? forcedLowerBound
        : gasPrice.compareTo(forcedUpperBound) >= 0 ? forcedUpperBound : gasPrice;
  }

  private Wei calculateBound(final Wei price, final long coefficient) {
    return price.multiply(coefficient).divide(100);
  }

  private Bytes callRemoteGasPrice(final Bytes function) {
    final CallParameter callParameter =
        new CallParameter(null, Address.GASPRICE, -1, null, null, function);
    final Optional<TransactionSimulatorResult> result =
        transactionSimulator.process(
            callParameter,
            ImmutableTransactionValidationParams.builder()
                .from(TransactionValidationParams.transactionSimulator())
                .isAllowExceedingBalance(true)
                .build(),
            OperationTracer.NO_TRACING,
            blockchainQueries.headBlockNumber());
    if (result.isEmpty()) {
      return BYTES_ZERO;
    }
    return result.get().getOutput();
  }
}
