package org.polkadot.api.derive.chain;

import com.google.common.collect.Lists;
import com.onehilltech.promises.Promise;
import org.polkadot.api.Types.ApiInterfacePromise;
import org.polkadot.api.Types.QueryableModuleStorage;
import org.polkadot.api.derive.Types;
import org.polkadot.types.type.AccountId;
import org.polkadot.types.type.BlockNumber;
import org.polkadot.types.type.Header;

import java.math.BigInteger;
import java.util.List;

public class ChainFunctions {


    /**
     * Get the latest block number.
     * **example**  
     * 
     * ```java
     * api.derive.chain.bestNumber((blockNumber) => {
     * System.out.print("the current best block is ");
     * System.out.println(blockNumber);
     * });
     * ```
     */
    public static Types.DeriveRealFunction bestNumber(ApiInterfacePromise api) {
        return new Types.DeriveRealFunction() {
            //(): Observable<BlockNumber> =>
            @Override
            public Promise call(Object... args) {

                return api.rpc().chain().function("subscribeNewHead").invoke()
                        .then(result -> {
                            Header header = (Header) result;
                            if (header != null && header.getBlockNumber() != null) {
                                return Promise.value(header.getBlockNumber());
                            }
                            //TODO 2019-05-25 00:25
                            throw new UnsupportedOperationException();
                        });
            }
        };
    }


    /**
     * Get the latest finalised block number.
     * **example**  
     * 
     * ```java
     * api.derive.chain.bestNumberFinalized((blockNumber) => {
     *     System.out.print("the current finalised block is ");
     *     System.out.print(blockNumber);
     * });
     * ```
     */
    public static Types.DeriveRealFunction bestNumberFinalized(ApiInterfacePromise api) {
        return new Types.DeriveRealFunction() {
            // (): Observable<BlockNumber> =>
            @Override
            public Promise call(Object... args) {
                return api.rpc().chain().function("subscribeFinalizedHeads").invoke()
                        .then(result -> {
                            Header header = (Header) result;
                            if (header != null && header.getBlockNumber() != null) {
                                return Promise.value(header.getBlockNumber());
                            }
                            //TODO 2019-05-25 00:25
                            throw new UnsupportedOperationException();
                        });
            }
        };
    }


    /**
     * Calculates the lag between finalised head and best head
     * **example**  
     * 
     * ```java
     * api.derive.chain.bestNumberLag((lag) => {
     *     System.out.printf("finalised is %d blocks behind head", lag);
     * });
     * ```
     */
    public static Types.DeriveRealFunction bestNumberLag(ApiInterfacePromise api) {
        return new Types.DeriveRealFunction() {
            //(): Observable<BlockNumber> =>
            @Override
            public Promise call(Object... args) {
                return Promise.all(
                        bestNumber(api).call(),
                        bestNumberFinalized(api).call()
                ).then(results -> {
                    BlockNumber bestNumber = (BlockNumber) results.get(0);
                    BlockNumber bestNumberFinalized = (BlockNumber) results.get(1);
                    BigInteger subtract = bestNumber.subtract(bestNumberFinalized);
                    BlockNumber ret = new BlockNumber(subtract);
                    return Promise.value(ret);
                });
            }
        };
    }

    /**
     * Get the a specific block header and extend it with the author
     */
    public static Types.DeriveRealFunction getHeader(ApiInterfacePromise api) {
        return new Types.DeriveRealFunction() {
            //(hash: Uint8Array | string): Observable<HeaderExtended | undefined> =>
            @Override
            public Promise call(Object... args) {
                Object hash = args[0];

                QueryableModuleStorage<Promise> session = api.query().section("session");

                return Promise.all(
                        api.rpc().chain().function("getHeader").invoke(hash),
                        session == null
                                ? session.function("validators").at(hash, null)
                                : Promise.value(Lists.newArrayList())
                ).then(results -> {
                    Header header = (Header) results.get(0);
                    List<AccountId> validators = (List<AccountId>) results.get(1);

                    Header.HeaderExtended headerExtended = new Header.HeaderExtended(header, validators);
                    return Promise.value(headerExtended);
                })._catch(err -> {
                    // where rpc.chain.getHeader throws, we will land here - it can happen that
                    // we supplied an invalid hash. (Due to defaults, storeage will have an
                    // empty value, so only the RPC is affected). So return undefined
                    return Promise.value(null);
                });
            }
        };
    }


    //export type HeaderAndValidators = [Header, Array<AccountId>];
    public static class HeaderAndValidators {
        Header header;
        List<AccountId> accountIds;
    }

    /**
     * Subscribe to block headers and extend it with the author
     * ```
     */
    public static Types.DeriveRealFunction subscribeNewHead(ApiInterfacePromise api) {
        return new Types.DeriveRealFunction() {
            // (): Observable<HeaderExtended> =>
            @Override
            public Promise call(Object... args) {
                return api.rpc().chain().function("subscribeNewHead").invoke()
                        .then(result -> {
                            Header header = (Header) result;
                            QueryableModuleStorage<Promise> session = api.query().section("session");
                            return Promise.all(
                                    Promise.value(header),
                                    session == null
                                            ? session.function("validators").at(header.getHash(), null)
                                            : Promise.value(Lists.newArrayList())

                            ).then(results -> {
                                Header header2 = (Header) results.get(0);
                                List<AccountId> validators = (List<AccountId>) results.get(1);

                                Header.HeaderExtended headerExtended = new Header.HeaderExtended(header2, validators);
                                return Promise.value(headerExtended);
                            });
                        });
            }
        };
    }

}
