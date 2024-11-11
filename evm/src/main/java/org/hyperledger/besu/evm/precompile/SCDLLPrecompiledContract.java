/*
 * Copyright contributors to Hyperledger Besu.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import java.util.Optional;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCDLLPrecompiledContract extends AbstractPrecompiledContract {

  private static final Logger LOG = LoggerFactory.getLogger(SCDLLPrecompiledContract.class);

  // TODO Define MAX_SIZE for safety,
  // we should not allowing iterate the list and take too long.
  // case 100ms 1/10 of 1 sec block time.
  // case 250ms 1/4 of 1 sec block time. based on blocktime of arbitrum and unichain
  // case 750ms 3/4 of 1 sec block time.
  // case 1000ms 1 sec block time.

  /** The constant MAX_SIZE. */
  static final Bytes MAX_SIZE =
      Bytes.fromHexString(
          "0x000000000000000000000000000000000000000000000000000000000000FFFF"); // max uint16

  /** The constant SENTINEL. */
  static final Bytes SENTINEL =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  /** The constant FALSE. */
  static final Bytes FALSE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  /** The constant TRUE. */
  public static final Bytes TRUE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");

  // CALL FUNCTION SIGNATURE (mutate state)
  // private static final Bytes REMOVE_SIGNATURE =
  //     Hash.keccak256(Bytes.of("remove(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes INSERT_SIGNATURE =
      Hash.keccak256(Bytes.of("insert(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);

  // STATIC CALL FUNCTION SIGNATURE (read)
  private static final Bytes BACK_SIGNATURE =
      Hash.keccak256(Bytes.of("back(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes FIND_SIGNATURE =
      Hash.keccak256(Bytes.of("find(uint256, uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes FRONT_SIGNATURE =
      Hash.keccak256(Bytes.of("front(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes LIST_SIGNATURE =
      Hash.keccak256(Bytes.of("list(uint256)".getBytes(UTF_8))).slice(0, 4);
  // private static final Bytes REVERSE_LIST_SIGNATURE =
  //     Hash.keccak256(Bytes.of("rlist(uint256)".getBytes(UTF_8))).slice(0, 4);
  // private static final Bytes MIDDLE_SIGNATURE =
  //     Hash.keccak256(Bytes.of("middle(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes NEXT_SIGNATURE =
      Hash.keccak256(Bytes.of("next(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes PREVIOUS_SIGNATURE =
      Hash.keccak256(Bytes.of("previous(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes SIZE_SIGNATURE =
      Hash.keccak256(Bytes.of("size(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes MAX_SIZE_SIGNATURE =
      Hash.keccak256(Bytes.of("max_size(uint256)".getBytes(UTF_8))).slice(0, 4);

  public SCDLLPrecompiledContract(final GasCalculator gasCalculator) {
    super("SCDLLPrecompiledContract", gasCalculator);
  }

  // calculateStorageSlot for the element
  private UInt256 calculateStorageSlot(
      final Address address, final Bytes listId, final Bytes element, final boolean direction) {
    // Include the listId and direction (0 for prev, 1 for next) as part of the key
    final Bytes directionBytes = direction ? TRUE : FALSE;
    final Bytes slotKey = Bytes.concatenate(address, listId, element, directionBytes);
    // Apply keccak256 to the combined key to get the storage slot
    // Convert the resulting 32-byte hash into a UInt256
    LOG.info("calculateStorageSlot slot: {}", UInt256.fromBytes(Bytes32.leftPad(Hash.keccak256(slotKey))));

    return UInt256.fromBytes(Bytes32.leftPad(Hash.keccak256(slotKey)));
  }

  // STATIC CALL FUNCTION
  private Bytes find(final MutableAccount account, final Bytes calldata) {
    // TODO:DLP
    // calldata must carrying listId and element
    if (calldata.size() < 64) {
      return FALSE;
    }
    final Address address = account.getAddress();
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32))); // Left pad
    final UInt256 element = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(32))); // Left pad
    final UInt256 previousNodeSlot = calculateStorageSlot(address, listId, element, false);
    final UInt256 nextNodeSlot = calculateStorageSlot(address, listId, element, true);
    final UInt256 previousNode = account.getStorageValue(previousNodeSlot);
    final UInt256 nextNode = account.getStorageValue(nextNodeSlot);
    return (nextNode.equals(element) || previousNode.compareTo(UInt256.ZERO) > 0) ? TRUE : FALSE;
  }

  private Bytes front(final MutableAccount account, final Bytes calldata) {
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32)));
    return account.getStorageValue(
        calculateStorageSlot(account.getAddress(), listId, SENTINEL, true));
  }

  private Bytes back(final MutableAccount account, final Bytes calldata) {
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32)));
    return account.getStorageValue(
        calculateStorageSlot(account.getAddress(), listId, SENTINEL, false));
  }

  // private Bytes middle(final MutableAccount account) {
  // TODO:DLP
  // calldata must carrying the listId then return the middle element of given listId
  // traversal half of size
  // return account.getStorageValueCalculateStorageSlot(account, key));
  //   return account.getStorageValue(UInt256.ZERO);
  // }

  private Bytes next(final MutableAccount account, final Bytes calldata) {
    if (calldata.size() < 64) {
      // Invalid input, not enough data for two uint256 values
      return FALSE;
    }
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32))); // Left pad
    final UInt256 element = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(32))); // Left pad
    return account.getStorageValue(
        calculateStorageSlot(account.getAddress(), listId, element, true));
  }

  private Bytes previous(final MutableAccount account, final Bytes calldata) {
    if (calldata.size() < 64) {
      // Invalid input, not enough data for two uint256 values
      return FALSE;
    }
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32))); // Left pad
    final UInt256 element = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(32))); // Left pad
    return account.getStorageValue(
        calculateStorageSlot(account.getAddress(), listId, element, false));
  }

  private Bytes size(final MutableAccount account, final Bytes calldata) {
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32)));
    if (calldata.size() < 32) {
      // Invalid input, not enough data for one uint256 values
      return FALSE;
    }
    return account.getStorageValue(UInt256.fromBytes(Hash.keccak256(listId)));
  }

  private Bytes maxSize() {
    return MAX_SIZE;
  }

  private Bytes list(final MutableAccount account, final Bytes calldata) {
    if (calldata.size() < 32) {
      return FALSE; // Invalid input
    }
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32)));
    final Address address = account.getAddress();
    UInt256 current = UInt256.fromBytes(front(account, calldata));
    if (current.equals(SENTINEL)) {
      return Bytes.EMPTY;
    }
    Bytes data = Bytes.EMPTY;
    int count = 0;
    while (!current.equals(SENTINEL) && UInt256.valueOf(count).compareTo(MAX_SIZE) < 0) {
      data = Bytes.concatenate(data, current); // Concatenate `current` into `data`
      current =
          UInt256.fromBytes(
              account.getStorageValue(calculateStorageSlot(address, listId, current, true)));
      count++;
    }
    return data;
  }

  // private Bytes reverseList(final MutableAccount account, final Bytes calldata) {
  //   if (calldata.size() < 32) {
  //     return FALSE; // Invalid input, not enough data for listId
  //   }
  //   final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32)));
  //   // Loop prev from back
  //   return account.getStorageValue(UInt256.ZERO);
  // }

  // CALL FUNCTION
  private Bytes insert(final MutableAccount account, final Bytes calldata) {
    if (calldata.size() < 64) {
      // Invalid input, not enough data for two uint256 values
      return FALSE;
    }
    final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32))); // Left pad
    final UInt256 element = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(32))); // Left pad
    LOG.info("listId {}",listId);
    LOG.info("element {}",element);
    final boolean exists = find(account, calldata).equals(TRUE);
    final UInt256 maxSize = UInt256.fromBytes(MAX_SIZE);
    final Address address = account.getAddress();
    UInt256 listSize = UInt256.fromBytes(size(account, listId));
    if (exists || listSize.compareTo(maxSize) >= 0) {
      return FALSE; // Element exists or size limit exceeded
    }
    UInt256 front = UInt256.fromBytes(front(account, calldata));
    UInt256 back = UInt256.fromBytes(back(account, calldata));
    UInt256 previousSentinelSlot = calculateStorageSlot(address, listId, SENTINEL, false);
    UInt256 nextSentinelSlot = calculateStorageSlot(address, listId, SENTINEL, true);
    UInt256 previousNodeSlot = calculateStorageSlot(address, listId, element, false);
    UInt256 nextNodeSlot = calculateStorageSlot(address, listId, element, true);
    if (listSize.equals(UInt256.ZERO)) {
      account.setStorageValue(previousSentinelSlot, element);
      account.setStorageValue(nextSentinelSlot, element);
      account.setStorageValue(previousNodeSlot, UInt256.fromBytes(SENTINEL));
      account.setStorageValue(nextNodeSlot, UInt256.fromBytes(SENTINEL));
    } else if (element.compareTo(front) < 0) {
      UInt256 oldFrontSlot = calculateStorageSlot(address, listId, front, false);
      account.setStorageValue(nextSentinelSlot, element);
      account.setStorageValue(oldFrontSlot, element);
      account.setStorageValue(previousNodeSlot, UInt256.fromBytes(SENTINEL));
      account.setStorageValue(nextNodeSlot, front);

    } else if (element.compareTo(back) > 0) {
      UInt256 oldBackSlot = calculateStorageSlot(address, listId, front, true);
      account.setStorageValue(previousSentinelSlot, element);
      account.setStorageValue(oldBackSlot, element);
      account.setStorageValue(previousNodeSlot, back);
      account.setStorageValue(nextNodeSlot, UInt256.fromBytes(SENTINEL));
    } else {
      UInt256 current = front;
      while (current.compareTo(back) < 0) {
        UInt256 next =
            UInt256.fromBytes(
                account.getStorageValue(calculateStorageSlot(address, listId, current, true)));
        if (element.compareTo(current) > 0 && element.compareTo(next) < 0) {
          // We found the correct position to insert between `current` and `next`
          UInt256 previous =
              UInt256.fromBytes(
                  account.getStorageValue(calculateStorageSlot(address, listId, current, false)));

          // Insert element between `current` and `next`
          account.setStorageValue(previousNodeSlot, previous);
          account.setStorageValue(nextNodeSlot, next);
          account.setStorageValue(calculateStorageSlot(address, listId, current, true), element);
          account.setStorageValue(calculateStorageSlot(address, listId, next, false), element);
          account.setStorageValue(calculateStorageSlot(address, listId, element, false), previous);
          account.setStorageValue(calculateStorageSlot(address, listId, element, true), next);
          break;
        }
        current = next;
      }
    }
    LOG.info("Updated list size storage slot: {}, New list size: {}", UInt256.fromBytes(Hash.keccak256(listId)), listSize.add(1L));
    account.setStorageValue(UInt256.fromBytes(Hash.keccak256(listId)), listSize.add(1L));
    return TRUE;
  }

  // private Bytes remove(final MutableAccount account, final Bytes calldata) {
  //   // TODO:DLP
  //   // calldata must carrying the listId and element then perform remove
  //   if (calldata.size() < 64) {
  //     // Invalid input, not enough data for two uint256 values
  //     return FALSE;
  //   }
  //   final UInt256 listId = UInt256.fromBytes(Bytes32.leftPad(calldata.slice(0, 32)));
  //   final UInt256 newElement = UInt256.fromBytes(calldata.slice(32, 64));
  //   UInt256 listSize = UInt256.fromBytes(size(account, listId));
  //   if (find(account, calldata).compareTo(FALSE) == 0) {
  //     return FALSE;
  //   } else {
  //     // do remove
  //     // size--;
  //     account.setStorageValue(UInt256.fromBytes(Hash.keccak256(listId)), listSize.subtract(1L));
  //     return TRUE;
  //   }
  // }

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (
    /*function.equals(REMOVE_SIGNATURE)*/
    function.equals(INSERT_SIGNATURE) || function.equals(LIST_SIGNATURE)
    /*|| function.equals(REVERSE_LIST_SIGNATURE)*/ ) {
      // for call function should be high calculate from time and space complexity of algorithms
      return 3000L;
    } else {
      // for static call function should be low, find the optimal number
      return 1500L;
    }
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
    final MutableAccount account = worldUpdater.getOrCreate(messageFrame.getSenderAddress());
    if (input.isEmpty() || account.getCode().isEmpty()) {
      // if the input calldata is empty or sender is EOA account return error.
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
    final Bytes function = input.slice(0, 4); // slice for function selector
    final Bytes calldata = input.slice(4); // slice for calldata
    LOG.info("function selector {}",function);
    LOG.info("calldata {}",calldata);
    LOG.info("calldata {}",calldata.size());

    if (function.equals(BACK_SIGNATURE)) {
      return PrecompileContractResult.success(back(account, calldata));
    } else if (function.equals(FIND_SIGNATURE)) {
      return PrecompileContractResult.success(find(account, calldata));
    } else if (function.equals(FRONT_SIGNATURE)) {
      return PrecompileContractResult.success(front(account, calldata));
    } else if (function.equals(LIST_SIGNATURE)) {
      return PrecompileContractResult.success(list(account, calldata));
    } else if (function.equals(MAX_SIZE_SIGNATURE)) {
      return PrecompileContractResult.success(maxSize());
    } else if (function.equals(NEXT_SIGNATURE)) {
      return PrecompileContractResult.success(next(account, calldata));
    } else if (function.equals(PREVIOUS_SIGNATURE)) {
      return PrecompileContractResult.success(previous(account, calldata));
    } else if (function.equals(INSERT_SIGNATURE)) {
      return PrecompileContractResult.success(insert(account, calldata));
      // } else if (function.equals(REMOVE_SIGNATURE)) {
      //   return PrecompileContractResult.success(remove(account, calldata));
      // } else if (function.equals(REVERSE_LIST_SIGNATURE)) {
      //   return PrecompileContractResult.success(reverseList(account, calldata));
    } else if (function.equals(SIZE_SIGNATURE)) {
      return PrecompileContractResult.success(size(account, calldata));
    } else {
      LOG.trace("Invalid function selector.");
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
  }
}
