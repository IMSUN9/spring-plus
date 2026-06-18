package org.example.expert.domain.todo.repository;

import java.util.Optional;

import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TodoRepositoryImpl implements TodoRepositoryCustom {

	private final JPAQueryFactory jpaQueryFactory;

	@Override
	public Optional<Todo> findByIdWithUser(Long todoId) {
		QTodo todo = QTodo.todo;
		QUser user = QUser.user;

		Todo result = jpaQueryFactory
			.selectFrom(todo)
			.leftJoin(todo.user, user).fetchJoin()
			.where(todo.id.eq(todoId))
			.fetchOne();

		return Optional.ofNullable(result);
	}
}
