package org.openmrs.api.hibernate;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.context.Context;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class HibernateUserService extends HibernateDaoSupport implements
		UserService {

	protected final Log log = LogFactory.getLog(getClass());
	
	private Context context;
	
	public HibernateUserService(Context c) {
		this.context = c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.api.UserService#createUser(org.openmrs.User)
	 */
	public void createUser(User user) {
		Session session = HibernateUtil.currentSession();
		Transaction tx = session.beginTransaction();
		
		user.setDateCreated(new Date());
		user.setCreator(context.getAuthenticatedUser());
		session.save(user);
		
		tx.commit();
		HibernateUtil.disconnectSession();
	}

	public User getUserByUsername(String username) {
		Session session = HibernateUtil.currentSession();

		List<User> users = session
				.createQuery(
						"from User u where (u.voided is null or u.voided = 0) and u.username = ?")
				.setString(0, username).list();

		HibernateUtil.disconnectSession();
		
		if (users == null || users.size() == 0) {
			log.warn("request for username '" + username + "' not found");
			return null;
			//throw new ObjectRetrievalFailureException(User.class, username);
		}

		return users.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.api.UserService#getUser(java.lang.Long)
	 */
	public User getUser(Integer userId) {
		Session session = HibernateUtil.currentSession();
		User user = (User) session.get(User.class, userId);
		
		HibernateUtil.disconnectSession();
		
		if (user == null) {
			log.warn("request for user '" + userId + "' not found");
			throw new ObjectRetrievalFailureException(User.class, userId);
		}
		return user;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.api.UserService#updateUser(org.openmrs.User)
	 */
	public void updateUser(User user) {
		if (user.getCreator() == null)
			createUser(user);
		else {
			if (log.isDebugEnabled()) {
				log.debug("update user id: " + user.getUserId());
			}
			Session session = HibernateUtil.currentSession();
			Transaction tx = session.beginTransaction();
	
			log.debug("### pre-save middle name = " + user.getMiddleName());
			session.saveOrUpdate(user);
			log.debug("### post-save middle name = " + user.getMiddleName());
	
			// flush to ensure any problems are handled immediately
			session.flush();
			log.debug("### post-flush middle name = " + user.getMiddleName());
	
			tx.commit();
			HibernateUtil.disconnectSession();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.api.UserService#voidUser(org.openmrs.User,
	 *      java.lang.String)
	 */
	public void voidUser(User user, String reason) {
		user.setVoided(true);
		user.setVoidReason(reason);
		user.setVoidedBy(context.getAuthenticatedUser());
		user.setDateVoided(new Date());
		updateUser(user);
	}

	public void deleteUser(User user) {
		Session session = HibernateUtil.currentSession();
		Transaction tx = session.beginTransaction();
		
		session.delete(user);
		
		tx.commit();
		HibernateUtil.disconnectSession();
	}

	public List findPatient(String q) {

		return getHibernateTemplate().find(
				"from Patient as p, PatientName as pn "
						+ "where p.patientId = pn.patientId "
						+ "and pn.familyName like ?", new Object[] { q });

	}

	/**
	 * @see org.openmrs.api.UserService#grantUserRole(org.openmrs.User,
	 *      org.openmrs.Role)
	 */
	public void grantUserRole(User user, Role role) throws APIException {
		Session session = HibernateUtil.currentSession();
		Transaction tx = session.beginTransaction();
		
		user.addRole(role);
		session.saveOrUpdate(user);
		
		tx.commit();
		HibernateUtil.disconnectSession();
	}

	/**
	 * @see org.openmrs.api.UserService#revokeUserRole(org.openmrs.User,
	 *      org.openmrs.Role)
	 */
	public void revokeUserRole(User user, Role role) throws APIException {
		Session session = HibernateUtil.currentSession();
		Transaction tx = session.beginTransaction();
		
		user.removeRole(role);
		session.saveOrUpdate(user);
		
		tx.commit();
		HibernateUtil.disconnectSession();

	}

	/**
	 * @see org.openmrs.api.UserService#unvoidUser(org.openmrs.User)
	 */
	public void unvoidUser(User user) throws APIException {
		user.setVoided(false);
		user.setVoidReason("");
		user.setVoidedBy(null);
		user.setDateVoided(null);
		updateUser(user);
	}

	/**
	 * @see org.openmrs.api.UserService#getPrivileges()
	 */
	public List<Privilege> getPrivileges() throws APIException {
		
		Session session = HibernateUtil.currentSession();
		
		List<Privilege> privileges = session.createQuery("from Privilege p").list();
		
		HibernateUtil.disconnectSession();
		
		return privileges;
	}

	/**
	 * @see org.openmrs.api.UserService#getRoles()
	 */
	public List<Role> getRoles() throws APIException {

		Session session = HibernateUtil.currentSession();
		
		List<Role> roles = session.createQuery("from Role r").list();
		
		HibernateUtil.disconnectSession();
		
		return roles;
	}
	
	

}
