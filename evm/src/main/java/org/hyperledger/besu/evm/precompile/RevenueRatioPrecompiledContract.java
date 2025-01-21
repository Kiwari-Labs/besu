/*
 * Copyright Advanced Info Services PCL.
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
package org.hyperledger.besu.evm.precompile;

import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RevenueRatioPrecompiledContract extends AbstractPrecompiledContract {
  private static final Logger LOG = LoggerFactory.getLogger(RevenueRatioPrecompiledContract.class);

  /** Ownable */
  private static final Bytes OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("owner()".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes INITIALIZED_SIGNATURE =
      Hash.keccak256(Bytes.of("initialized()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes INITIALIZE_OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("initializeOwner(address)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes TRANSFER_OWNERSHIP_SIGNATURE =
      Hash.keccak256(Bytes.of("transferOwnership(address)".getBytes(UTF_8))).slice(0, 4);

  /** Status */
  private static final Bytes ENABLE_SIGNATURE =
      Hash.keccak256(Bytes.of("enable()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes DISABLE_SIGNATURE =
      Hash.keccak256(Bytes.of("disable()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes STATUS_SIGNATURE =
      Hash.keccak256(Bytes.of("status()".getBytes(UTF_8))).slice(0, 4);

  /** RevenueRatio */
  private static final Bytes CONTRACT_RATIO_SIGNATURE =
      Hash.keccak256(Bytes.of("contractRatio()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes COINBASE_RATIO_SIGNATURE =
      Hash.keccak256(Bytes.of("coinbaseRatio()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes PROVIDER_RATIO_SIGNATURE =
      Hash.keccak256(Bytes.of("providerRatio()".getBytes(UTF_8))).slice(0, 4);

  /** Storage Layout */
  private static final UInt256 INIT = UInt256.ZERO;

  private static final UInt256 OWNER = UInt256.ONE;

  private static final UInt256 CONTRACT_RATIO = Uint256.valueOf(2L);

  private static final UInt256 COINBASE_RATIO = Uint256.valueOf(3L);

  private static final UInt256 PROVIDER_RATIO = Uint256.valueOf(4L);

  /** Returns */
  private static final Bytes FALSE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  private static final Bytes TRUE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");

  public RevenueRatioPrecompiledContract(final GasCalculator gasCalculator) {
    super("RevenueRatioPrecompiledContract", gasCalculator);
  }

  /** Modifier */
  private Bytes onlyOwner(final MutableAccount contract, Address senderAddress) {
    final Address storedOwner = Address.wrap(contract.getStorageValue(OWNER));
    if (storedOwner.equals(senderAddress)) {
      return TRUE;
    } else {
      return FALSE;
    }
  }

  private Bytes owner(final MutableAccount contract) {
    return contract.getStorageValue(OWNER);
  }

  private Bytes initialized(final MutableAccount contract) {
    return contract.getStorageValue(INIT);
  }

  private Bytes initializeOwner(final MutableAccount contract, Address senderAddress, final Bytes calldata) {
    if (initialized(contract).equals(ONE)) {
      return FALSE;
    } else {
      contract.setStorageValue(INIT, UInt256.ONE);
    // extract/slice address from messageFrame
    // contract.setStorageValue(OWNER, initialOwner);
    return TRUE;
    }
  }

  private Bytes transferOwnership(final MutableAccount contract, Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, messageFrame).equals(ONE)) {
      return FALSE;
    } else {
    // extract/slice address from messageFrame
    // contract.setStorageValue(OWNER, neOwner);
      return TRUE;
    }
  }

  /** @TODO interface */

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (function.equals(OWNER_SIGNATURE) || function.equals(INITIALIZED_SIGNATURE)) {
      // gas cost for read operation.
      return 1000;
    } else {
      // gas const for read operation.
      return 2000;
    }
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    // @TODO
  }
}
