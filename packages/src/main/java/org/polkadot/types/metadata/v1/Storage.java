package org.polkadot.types.metadata.v1;

import org.polkadot.types.Types;
import org.polkadot.types.TypesUtils;
import org.polkadot.types.codec.EnumType;
import org.polkadot.types.codec.Struct;
import org.polkadot.types.codec.Vector;
import org.polkadot.types.primitive.*;

public interface Storage {

    class Default extends Null {
    }

    class Optional extends Null {
    }

    //<Optional | Default>
    class MetadataStorageModifier extends EnumType {

        public MetadataStorageModifier(Object value) {
            this(value, -1);
        }

        public MetadataStorageModifier(Object value, int index) {
            super(new Types.ConstructorDef()
                            .add("Optional", Optional.class)
                            .add("Default", Default.class)
                    , value, index, null);
        }

        /**
         * `true` if the storage entry is optional
         */
        public boolean isOptional() {
            return this.toNumber() == 0;
        }

        @Override
        public Object toJson() {
            return this.toString();
        }
    }


    class MapType extends Struct {
        private boolean isLinked = false;

        public MapType(Object value) {
            super(new Types.ConstructorDef()
                            .add("key", Type.class)
                            .add("value", Type.class)
                    , value);

            //MapType v0 v1 v2
            if (value instanceof Struct) {
                Bool isLinked = ((Struct) value).getField("isLinked");
                if (isLinked != null) {
                    this.isLinked = isLinked.rawBool();
                }
            }
        }


        /**
         * The mapped key as {@link org.polkadot.types.type}
         */
        public Type getKey() {
            return this.getField("key");
        }

        /**
         * The mapped value as {@link org.polkadot.types.type}
         */
        public Type getValue() {
            return this.getField("value");
        }

        /**
         * Is this an enumerable linked map
         */
        public Boolean isLinked() {
            return this.isLinked;
        }
    }

    class PlainType extends Type {
        public PlainType(Object value) {
            super(value);
        }
    }


    //<PlainType | MapType>
    class MetadataStorageType extends EnumType {

        public MetadataStorageType(Object value) {
            this(value, -1);
        }

        public MetadataStorageType(Object value, int index) {
            super(new Types.ConstructorDef()
                            .add("PlainType", PlainType.class)
                            .add("MapType", MapType.class)
                    , value, index, null);
        }

        /**
         * `true` if the storage entry is a map
         */
        public boolean isMap() {
            return this.toNumber() == 1;
        }

        /**
         * The value as a mapped value
         */
        public MapType asMap() {
            return (MapType) this.value();
        }

        /**
         * The value as a {@link org.polkadot.types.type} value
         */
        public PlainType asType() {
            return (PlainType) this.value();
        }

        /**
         * Returns the string representation of the value
         */


        @Override
        public String toString() {
            return this.isMap()
                    ? this.asMap().getValue().toString() :
                    this.asType().toString();
        }
    }


    /**
     * The definition of a storage function
     */
    class MetadataStorageV1 extends Struct {
        public MetadataStorageV1(Object value) {
            super(new Types.ConstructorDef()
                            .add("name", Text.class)
                            .add("modifier", MetadataStorageModifier.class)
                            .add("type", MetadataStorageType.class)
                            .add("fallback", Bytes.class)
                            .add("docs", Vector.with(TypesUtils.getConstructorCodec(Text.class)))
                    , value);
        }

        /**
         * The {@link org.polkadot.types.primitive.Text} documentation
         */
        public Vector<Text> getDocs() {
            return this.getField("docs");
        }

        /**
         * The {@link org.polkadot.types.primitive.Bytes} fallback default
         */
        public Bytes getFallback() {
            return this.getField("fallback");
        }

        /**
         * The MetadataArgument for arguments
         */
        public MetadataStorageModifier getModifier() {
            return this.getField("modifier");
        }

        /**
         * The call name
         */
        public Text getName() {
            return this.getField("name");
        }

        /**
         * The MetadataStorageType
         */
        public MetadataStorageType getType() {
            return this.getField("type");
        }
    }

}
