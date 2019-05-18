package org.polkadot.types.rpc;

import org.polkadot.types.type.Extrinsics;

/**
 * @name PendingExtrinsics
 * @description
 * A list of pending [[Extrinsics]]
 */
public class PendingExtrinsics extends Extrinsics {
    public PendingExtrinsics(Object value) {
        super(value);
    }
}