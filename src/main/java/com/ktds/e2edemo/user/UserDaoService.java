package com.ktds.e2edemo.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

@Component
public class UserDaoService {
	private static List<User> users = new ArrayList<>();
	private static int usersCount = 3;
	
	static {
		users.add(new User(1, "Henry", LocalDate.now().minusYears(18)));
		users.add(new User(2, "Jane", LocalDate.now().minusYears(54)));
		users.add(new User(3, "Tom", LocalDate.now().minusYears(26)));
	}

	public List<User> findAll() {
		return users;
	}

	public User findOne(int id) {
		Predicate<? super User> predicate = user ->

		user.getId().equals(id);

		return users.stream().filter(predicate).findFirst().get();
	}

	public User save(User user) {
		user.setId(++usersCount);
		users.add(user);
		return user;
	}
}
