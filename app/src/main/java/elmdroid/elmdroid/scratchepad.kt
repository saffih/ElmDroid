package elmdroid.elmdroid


sealed class Maybe<out T> {
    object None : Maybe<Nothing>()
    data class Just<T>(val t: T) : Maybe<T>()
}

// https://discuss.kotlinlang.org/t/any-thoughts-on-ceylon-style-union-and-intersection-types/547/19
sealed class Either<out F, out S>
{
    object XXX: Either<Nothing, Nothing>()
    data class First<out F>(val value: F) : Either<F, Nothing>()
    data class Second<out S>(val value: S) : Either<Nothing, S>()
}
typealias Choice = Either<String, Int>

val e: Choice = Either.First("hi")
val a: Choice = Either.Second(5)

val s: String = when (e)
{
    is Either.First    -> e.value
    is Either.Second -> e.value.toString()
    Either.XXX -> TODO()
}
