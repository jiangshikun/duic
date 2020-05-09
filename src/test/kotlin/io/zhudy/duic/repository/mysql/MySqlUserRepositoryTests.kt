package io.zhudy.duic.repository.mysql

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.zhudy.duic.repository.BasicTestRelationalConfiguration
import io.zhudy.duic.repository.UserRepository
import io.zhudy.duic.vo.UserVo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import java.util.*

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@SpringBootTest(classes = [
    MySqlUserRepository::class
])
@ActiveProfiles("mysql")
@ImportAutoConfiguration(classes = [BasicTestRelationalConfiguration::class])
internal class MySqlUserRepositoryTests {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var transactionalOperator: TransactionalOperator

    @Test
    fun insert() {
        val email = "integration-test@mail.com"
        val password = "[PASSWORD]"

        val p = userRepository.insert(UserVo.NewUser(email = email, password = password))
        val n = transactionalOperator.transactional(p).block()
        assertThat(n).isEqualTo(1)
    }

    @Test
    fun `insert duplicate`() {
        val email = "integration-test@mail.com"
        val password = "[PASSWORD]"

        val p = userRepository.insert(UserVo.NewUser(email = email, password = password))
                .then(
                        userRepository.insert(UserVo.NewUser(email = email, password = password))
                )

        assertThatThrownBy { transactionalOperator.transactional(p).block() }
                .isInstanceOf(DataIntegrityViolationException::class.java)
                .hasCauseInstanceOf(R2dbcDataIntegrityViolationException::class.java)
                .hasMessageContaining("Duplicate")
    }

    @Test
    fun delete() {
        val email = "integration-test@mail.com"
        val password = "[PASSWORD]"

        val p = userRepository.insert(UserVo.NewUser(email = email, password = password))
                .then(
                        userRepository.delete(email)
                )
        val n = transactionalOperator.transactional(p).block()
        assertThat(n).isEqualTo(1)
    }

    @Test
    fun `delete not found`() {
        val email = "${UUID.randomUUID()}@mail.com"

        val p = userRepository.delete(email)
        val n = transactionalOperator.transactional(p).block()
        assertThat(n).isEqualTo(0)
    }

    @Test
    fun updatePassword() {
        val email = "integration-test@mail.com"
        val password = "[PASSWORD]"

        val p = userRepository.insert(UserVo.NewUser(email = email, password = password))
                .then(
                        userRepository.updatePassword(email, "[NEW-PASSWORD]")
                )
        val n = transactionalOperator.transactional(p).block()
        assertThat(n).isEqualTo(1)
    }

    @Test
    fun `updatePassword not found`() {
        val email = "${UUID.randomUUID()}@mail.com"
        val password = "[PASSWORD]"

        val p = userRepository.updatePassword(email, password)
        val n = transactionalOperator.transactional(p).block()
        assertThat(n).isEqualTo(0)
    }

    @Test
    fun findByEmail() {
        val email = "integration-test@mail.com"
        val password = "[PASSWORD]"

        val p = userRepository.insert(UserVo.NewUser(email = email, password = password))
                .then(
                        userRepository.findByEmail(email)
                )
        val u = transactionalOperator.transactional(p).block()
        assertThat(u).isNotNull
                .hasFieldOrPropertyWithValue("password", password)
                .hasFieldOrProperty("createdAt")
                .hasFieldOrProperty("updatedAt")
    }

    @Test
    fun `findByEmail not found`() {
        val email = "integration-test@mail.com"

        val p = userRepository.findByEmail(email)
        val u = transactionalOperator.transactional(p).block()
        assertThat(u).isNull()
    }

    @Test
    fun findPage() {
        // FIXME 这里有 bug 等待修复
        val c = 2
        val prepare = Flux.range(0, c).map {
            UserVo.NewUser(email = "integration-test$it@mail.com", password = "[PASSWORD]")
        }.flatMap(userRepository::insert)

        val n = transactionalOperator.transactional(prepare).blockLast()
        println(n)

        val pageRequest = PageRequest.of(0, 15)
//        val p = prepare.then(userRepository.findPage(pageRequest))
//
//        val page = transactionalOperator.transactional(p).block()
//        assertThat(page).isNotNull
    }

    @Test
    fun findAllEmail() {
        val c = 30
        val prepare = Flux.range(0, c).map {
            UserVo.NewUser(email = "integration-test$it@mail.com", password = "[PASSWORD]")
        }.flatMap(userRepository::insert)

        val list = prepare.thenMany(userRepository.findAllEmail())
                .`as`(transactionalOperator::transactional)
                .collectList()
                .block()
        assertThat(list).hasSizeGreaterThanOrEqualTo(c)
    }

}