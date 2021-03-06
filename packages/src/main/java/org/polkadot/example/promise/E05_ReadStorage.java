package org.polkadot.example.promise;

import com.onehilltech.promises.Promise;
import org.polkadot.api.Types;
import org.polkadot.api.promise.ApiPromise;
import org.polkadot.direct.IRpcFunction;
import org.polkadot.rpc.provider.ws.WsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class E05_ReadStorage {
    static String Alice = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY";

    //static String endPoint = "wss://poc3-rpc.polkadot.io/";
    //static String endPoint = "wss://substrate-rpc.parity.io/";
    //static String endPoint = "ws://45.76.157.229:9944/";
    static String endPoint = "ws://127.0.0.1:9944";

    static void initEndPoint(String[] args) {
        if (args != null && args.length >= 1) {
            endPoint = args[0];
            System.out.println(" connect to endpoint [" + endPoint + "]");
        } else {
            System.out.println(" connect to default endpoint [" + endPoint + "]");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create an await for the API
        //Promise<ApiPromise> ready = ApiPromise.create();
        initEndPoint(args);

        WsProvider wsProvider = new WsProvider(endPoint);

        Promise<ApiPromise> ready = ApiPromise.create(wsProvider);

        AtomicReference<ApiPromise> readyApi = new AtomicReference<>();

        List validators = new ArrayList();
        ready.then(api -> {
                    readyApi.set(api);
                    Types.QueryableStorage<Promise> query = api.query();
                    Types.QueryableModuleStorage<Promise> timestamp = query.section("timestamp");
                    Types.QueryableModuleStorage<Promise> system = query.section("system");
                    Types.QueryableModuleStorage<Promise> session = query.section("session");
                    return Promise.all(
                            system.function("accountNonce").call(Alice),
                            timestamp.function("blockPeriod").call(),
                            session.function("validators").call()
                    );
                }
        ).then(
                (result) -> {

                    System.out.println(result);

                    validators.add(result.get(2));
                    synchronized (validators) {
                        validators.notify();
                    }
                    //System.out.println(Arrays.toString(result));
                    return null;
                }
        )._catch((err) -> {
            err.printStackTrace();
            return Promise.value(err);
        });

        System.out.println("wait validators");
        synchronized (validators) {
            validators.wait();
        }

        List<Object> authorityIds = (List<Object>) validators.get(0);
        System.out.println("handle validators, authorityIds count = " + validators.size());

        ready.then(api -> {
            // Retrieve the initial balance. Since the call has no callback, it is simply a promise
            // that resolves to the current on-chain value
            Types.QueryableStorage<Promise> query = api.query();
            Types.QueryableModuleStorage<Promise> balances = query.section("balances");
            Types.QueryableStorageFunction<Promise> freeBalance = balances.function("freeBalance");

            List<Promise> collect = authorityIds.stream().map(
                    (authorityId) -> (Promise<IRpcFunction.Unsubscribe<Promise>>) freeBalance.call(authorityId.toString(),
                            (IRpcFunction.SubscribeCallback) o -> System.out.println("address : " + authorityId.toString() + ", freeBalance : " + o)


                    )).collect(Collectors.toList());
            return Promise.all(collect.toArray(new Promise[]{}));
        }).then((subResults) -> {
                    //System.out.println(" set unsubscribe " + subResults);
                    return null;
                }
        )._catch((err) -> {
            err.printStackTrace();
            return Promise.value(err);
        });

    }
}
