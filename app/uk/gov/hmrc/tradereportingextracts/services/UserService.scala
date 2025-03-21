package uk.gov.hmrc.tradereportingextracts.services

import play.api.Logging
import uk.gov.hmrc.tradereportingextracts.models.User
import uk.gov.hmrc.tradereportingextracts.repositories.UserRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(
                             userRepository: UserRepository
                           )(using ec: ExecutionContext) extends Logging:

  def insertUser(user: User)
                (using ec: ExecutionContext): Future[Boolean] =
    userRepository.insertUser(user)

  def findByUserId(userid: Long)(using ec: ExecutionContext): Future[Option[User]] =
    userRepository.findByUserId(userid)

  def updateByUserId(user: User): Future[Boolean] =
    userRepository.updateByUserId(user)

  def deleteByUserId(userid: Long): Future[Boolean] =
    userRepository.deleteByUserId(userid)