package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;

import org.reactfx.util.Either;

public interface Codec<T> {

    String getName();
    void encode(DataOutputStream os, T t) throws IOException;
    T decode(DataInputStream is) throws IOException;


    static final Codec<String> STRING_CODEC = new Codec<String>() {

        @Override
        public String getName() {
            return "string";
        }

        @Override
        public void encode(DataOutputStream os, String s) throws IOException {
            os.writeUTF(s);
        }

        @Override
        public String decode(DataInputStream is) throws IOException {
            return is.readUTF();
        }
    };

    static final Codec<Color> COLOR_CODEC = new Codec<Color>() {

        @Override
        public String getName() {
            return "color";
        }

        @Override
        public void encode(DataOutputStream os, Color c)
                throws IOException {
            os.writeDouble(c.getRed());
            os.writeDouble(c.getGreen());
            os.writeDouble(c.getBlue());
            os.writeDouble(c.getOpacity());
        }

        @Override
        public Color decode(DataInputStream is) throws IOException {
            return Color.color(
                    is.readDouble(),
                    is.readDouble(),
                    is.readDouble(),
                    is.readDouble());
        }

    };

    static <T> Codec<List<T>> listCodec(Codec<T> elemCodec) {
        return SuperCodec.collectionListCodec(elemCodec);
    }

    static <T> Codec<Collection<T>> collectionCodec(Codec<T> elemCodec) {
        return SuperCodec.upCast(SuperCodec.collectionListCodec(elemCodec));
    }

    static <T> Codec<Optional<T>> optionalCodec(Codec<T> elemCodec) {
        return new Codec<Optional<T>>() {

            @Override
            public String getName() {
                return "optional<" + elemCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, Optional<T> ot) throws IOException {
                if(ot.isPresent()) {
                    os.writeBoolean(true);
                    elemCodec.encode(os, ot.get());
                } else {
                    os.writeBoolean(false);
                }
            }

            @Override
            public Optional<T> decode(DataInputStream is) throws IOException {
                return is.readBoolean()
                        ? Optional.of(elemCodec.decode(is))
                        : Optional.empty();
            }

        };
    }

    static <E extends Enum<E>> Codec<E> enumCodec(Class<E> enumType) {
        return new Codec<E>() {

            @Override
            public String getName() {
                return enumType.getSimpleName();
            }

            @Override
            public void encode(DataOutputStream os, E e) throws IOException {
                os.writeInt(e.ordinal());
            }

            @Override
            public E decode(DataInputStream is) throws IOException {
                int ord = is.readInt();
                return enumType.getEnumConstants()[ord];
            }

        };
    }

    static <L, R> Codec<Either<L, R>> eitherCodec(Codec<L> lCodec, Codec<R> rCodec) {
        return new Codec<Either<L, R>>() {

            @Override
            public String getName() {
                return "either<" + lCodec.getName() + ", " + rCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, Either<L, R> e) throws IOException {
                if(e.isLeft()) {
                    os.writeBoolean(false);
                    lCodec.encode(os, e.getLeft());
                } else {
                    os.writeBoolean(true);
                    rCodec.encode(os, e.getRight());
                }
            }

            @Override
            public Either<L, R> decode(DataInputStream is) throws IOException {
                boolean isRight = is.readBoolean();
                return isRight
                        ? Either.right(rCodec.decode(is))
                        : Either.left (lCodec.decode(is));
            }
        };
    }
}