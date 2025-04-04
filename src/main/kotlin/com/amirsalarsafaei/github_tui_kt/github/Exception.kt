package com.amirsalarsafaei.github_tui_kt.github

sealed class FullUserRetrieveException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class SaveUserException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

class RetrieveFailed(message: String, cause: Throwable? = null) : FullUserRetrieveException(message, cause)

class SaveRepositoryException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
