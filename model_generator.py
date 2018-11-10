from keras.models import Model
from keras.layers import *
from custom_layers import Conv1DWithMasking, MeanOverTime


def generate_model(vocab_size, embeddings_size, max_essay_length, embeddings_matrix, loss_weights):
    inp = Input((2000,),)
    x = Embedding(input_dim=vocab_size,
                        output_dim=embeddings_size,
                        input_length=max_essay_length,
                        weights=[embeddings_matrix],
                        trainable=False,
                        mask_zero=True,) (inp)
    x = Conv1DWithMasking(filters=40,
                                kernel_size=3,
                                activation='sigmoid',
                                use_bias=True,
                                padding='same',) (x)
    x = LSTM(40, return_sequences=True, )(x)
    x = Dropout(0.1) (x)
    x = MeanOverTime(mask_zero=True) (x)
    out = []
    for i in range(len(loss_weights)):
        attribute_score = Dense(units=1, activation='sigmoid') (x)
        out.append(attribute_score)
    model = Model(inp, out)

    # compile the model
    model.compile(optimizer='rmsprop', loss=['mse' for _ in loss_weights], loss_weights=loss_weights)
    # summarize the model
    print(model.summary())

    return model
