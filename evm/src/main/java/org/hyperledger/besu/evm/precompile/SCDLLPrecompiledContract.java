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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulSortedCircularLinkedListPrecompiledContract
    extends AbstractPrecompiledContract {

  private static final Logger LOG =
      LoggerFactory.getLogger(StatefulSortedCircularLinkedListPrecompiledContract.class);

  // TODO Define MAX_SIZE for safety,
  // we should not allowing iterate the list and take too long.
  // case 100ms 1/10 of 1 sec block time.
  // case 250ms 1/4 of 1 sec block time. based on blocktime of arbitrum and unichain
  // case 750ms 3/4 of 1 sec block time.
  // case 1000ms 1 sec block time.

  // CONSTANT VARIABLE
  private static final long MAX_SIZE = 65535; // assume max uint16 is optimal number.
  private static final UInt256 ONE_BIT = UInt256.valueOf(1);
  private static final UInt256 SENTINEL = UInt256.valueOf(0);
  private static final UInt256 NEXT = UInt256.valueOf(1);
  private static final UInt256 PREVIOUS = UInt256.valueOf(0);
  private static final Bytes TRUE =
      UInt256.valueOf(0); // `one` can be decode to `true` in solidity type bool
  private static final Bytes FALSE =
      UInt256.valueOf(0); // `zero` can be decode to `false` in solidity type bool

  // CALL FUNCTION SIGNATURE
  private static final Bytes REMOVE_SIGNATURE =
      Hash.keccak256(Bytes.of("remove(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes INSERT_SIGNATURE =
      Hash.keccak256(Bytes.of("insert(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);

  // STATIC CALL FUNCTION SIGNATURE
  private static final Bytes FIND_SIGNATURE =
      Hash.keccak256(Bytes.of("find(uint256, uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes FRONT_SIGNATURE =
      Hash.keccak256(Bytes.of("front(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes BACK_SIGNATURE =
      Hash.keccak256(Bytes.of("back(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes LIST_SIGNATURE =
      Hash.keccak256(Bytes.of("list(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes REVERSE_LIST_SIGNATURE =
      Hash.keccak256(Bytes.of("rlist(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes MIDDLE_SIGNATURE =
      Hash.keccak256(Bytes.of("middle(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes NEXT_SIGNATURE =
      Hash.keccak256(Bytes.of("next(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes PREVIOUS_SIGNATURE =
      Hash.keccak256(Bytes.of("previous(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes SIZE_SIGNATURE =
      Hash.keccak256(Bytes.of("size(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes MAX_SIZE_SIGNATURE =
      Hash.keccak256(Bytes.of("max_size(uint256)".getBytes(UTF_8))).slice(0, 4);

  public StatefulSortedCircularLinkedListPrecompiledContract(final GasCalculator gasCalculator) {
    super("StatefulSortedCircularLinkedListPrecompiledContract", gasCalculator);
  }

  // TODO:DLP reuse blake2bf for storage slot.

  // CalculateStorageSLot for List size
  private UInt256 CalculateStorageSlotBlake2bf(
      final Address address, final Bytes key, final UInt256 index) {
    // Hash.blake2bf(address, key, index); // convert into UInt256
    return UInt256.valueOf(1);
  }

  // CalculateStorageSLot for Node
  private UInt256 CalculateStorageSlot(
      final Address address, final Bytes pointer, final UInt256 element, final UInt256 direction) {
    // Hash.keccak256(address, pointer, element, direction); // convert into UInt256
    return UInt256.valueOf(1);
  }

  // STATIC CALL FUNCTION
  private Bytes find(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying listId and element
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes front(final MutableAccount address) {
    // TODO:DLP calldata must carrying listId then return the first element of given listId
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes middle(final MutableAccount address) {
    // TODO:DLP calldata must carrying the listId then return the middle element of given listId
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes next(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId and element then return the next element
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes previous(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId and element then return the previous element
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes size(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId then return current size of the given listId
    // address.getStorageValue(CalculateStorageSlotBlake2bf(address, calldata, Uint256.valueOf(0)));
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes maxSize() {
    // TODO:DLP return for only contract that has been initialized list storage before, applied same
    // maxSize to all listId
    return Uint256.valueOf(MAX_SIZE);
  }

  private Bytes back(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId then return the last element of given listId
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes list(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId then return the list
    return address.getStorageValue(UInt256.ZERO);
  }

  private Bytes reverseList(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId and then return the list in reverse
    return address.getStorageValue(UInt256.ZERO);
  }

  // CALL FUNCTION
  private Bytes insert(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId and element then perform insert
    return FALSE;
  }

  private Bytes remove(final MutableAccount address, final Bytes calldata) {
    // TODO:DLP calldata must carrying the listId and element then perform remove
    return FALSE;
  }

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (function.equals(REMOVE_SIGNATURE)
        || function.equals(INSERT_SIGNATURE)
        || function.equals(LIST_SIGNATURE)
        || function.equals(REVERSE_LIST_SIGNATURE)) {
      // for call function should be high
      // TODO:DLP calculate from time and space complexity of algorithms
      // calculate from iterator need to process
      // size of list * static cost
      // 100 is base gasUsed preventing rounds equal to zero
      // return 100L + (size * 8);
      return 0L;
    } else {
      // for static call function should be low
      return 800L;
    }
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
    final MutableAccount sender = worldUpdater.getOrCreate(messageFrame.getSenderAddress());
    if (input.isEmpty() || sender.getCode().isEmpty()) {
      // if the input calldata is empty or sender is EOA account return error.
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
    final Bytes calldata = input;
    Map<Bytes, Function<MutableAccount, PrecompileContractResult>> handlers = new HashMap<>();
    handlers.put(FIND_SIGNATURE, (s) -> find(s, calldata));
    handlers.put(FRONT_SIGNATURE, (s) -> front(s, calldata));
    handlers.put(LIST_SIGNATURE, (s) -> list(s, calldata));
    handlers.put(REVERSE_LIST_SIGNATURE, (s) -> reverseList(s, calldata));
    handlers.put(MIDDLE_SIGNATURE, (s) -> middle(s, calldata));
    handlers.put(NEXT_SIGNATURE, (s) -> next(s, calldata));
    handlers.put(PREVIOUS_SIGNATURE, (s) -> previous(s, calldata));
    handlers.put(SIZE_SIGNATURE, (s) -> size(s, calldata));
    handlers.put(MAX_SIZE_SIGNATURE, (s) -> maxSize());
    handlers.put(BACK_SIGNATURE, (s) -> back(s, calldata));
    handlers.put(REMOVE_SIGNATURE, (s) -> remove(s, calldata));
    handlers.put(INSERT_SIGNATURE, (s) -> insert(s, calldata));

    // function selector
    final Bytes function = input.slice(0, 4);

    // fetch the appropriate handler based on the function selector
    Function<MutableAccount, PrecompileContractResult> handler = handlers.get(function);

    // reset the map reference to null after usage
    handlers = null;
    if (handler != null) {
      return PrecompileContractResult.success(handler.apply(sender));
    } else {
      LOG.trace("Failed interface id invalid");
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    }
  }
}
