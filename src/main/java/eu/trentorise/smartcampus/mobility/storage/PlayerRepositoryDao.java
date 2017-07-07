package eu.trentorise.smartcampus.mobility.storage;

import java.util.List;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.Player;

@Repository
public interface PlayerRepositoryDao extends CrudRepository<Player, String>{
	
	/*public Player findByPid(String id);
	
	public Player findBySocialId(String id);

	@Query("{'nickName': ?0}")
	public Player findByNick(String nickname);
	
	@Query("{'nickName': { '$regex': ?0, $options:'i'}}")
	public Player findByNickIgnoreCase(String nickname);*/
	
	Iterable<Player> findAll();
	
	Iterable<Player> findAllByCheckedRecommendation(boolean recommendation);
	
	public Player findByPid(String id);
	
	public Player findBySocialId(String id);

	@Query("{'nickname': ?0}")
	public Player findByNickname(String nickname);
	
	@Query("{'nickname': { '$regex': ?0, $options:'i'}}")
	public Player findByNicknameIgnoreCase(String nickname);

	@Query ("{'personalData.nicknameRecommandation': ?0}")
	public List<Player> findByNicknameRecommandation(String nickname);

}
