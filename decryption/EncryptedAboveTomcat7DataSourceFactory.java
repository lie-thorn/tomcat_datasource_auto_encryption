import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory;

import com.tomcat.datasource.encryption.EncodeProperties;

public class EncryptedAboveTomcat7DataSourceFactory extends BasicDataSourceFactory {
	private final String NEW_LINE = "\n";
	private final String CHAR_SET = "UTF-8";

	public String encode(String paramString, String secret) {
		try {
			return EncodeProperties.encrypt(paramString.replace("\n", ""), secret);
		} catch (Exception localException) {
			throw new RuntimeException(localException);
		}
	}

	public String decode(String paramString) {
		try {
			System.out.println("java.version:"+System.getProperty("java.version"));
			if (paramString.startsWith("Encrypted:")) {
				return EncodeProperties.decrypt(paramString.replace("Encrypted:", ""),
						System.getProperty("java.version"));
			}
			throw new RuntimeException("解密失败");
		} catch (Exception localException) {
			throw new RuntimeException(localException);
		}
	}

	// 关键修改：移除throws Exception声明
	public Object getObjectInstance(Object paramObject, Name paramName, Context paramContext, Hashtable<?, ?>  paramHashtable) {
		try { // 用try-catch捕获所有异常，转换为RuntimeException
			if ((paramObject instanceof Reference)) {
				System.out.println("解密用户名");
				setUsername((Reference) paramObject);
				System.out.println("解密口令");
				setPassword((Reference) paramObject);
			}
			return super.getObjectInstance(paramObject, paramName, paramContext, paramHashtable);
		} catch (Exception e) {
			// 将已检查异常转换为运行时异常（无需在方法声明中声明）
			throw new RuntimeException("getObjectInstance执行失败", e);
		}
	}

	private void setUsername(Reference paramReference) throws Exception {
		findDecryptAndReplace("username", paramReference);
	}

	private void setPassword(Reference paramReference) throws Exception {
		findDecryptAndReplace("password", paramReference);
	}

	private void findDecryptAndReplace(String paramString, Reference paramReference) throws Exception {
		int i = find(paramString, paramReference);
		String str = decrypt(i, paramReference);
		replace(i, paramString, str, paramReference);
	}

	private void replace(int paramInt, String paramString1, String paramString2, Reference paramReference)
			throws Exception {
		paramReference.remove(paramInt);
		paramReference.add(paramInt, new StringRefAddr(paramString1, paramString2));
	}

	private String decrypt(int paramInt, Reference paramReference) throws Exception {
		return decode(paramReference.get(paramInt).getContent().toString());
	}

	private int find(String paramString, Reference paramReference) throws Exception {
		Enumeration<RefAddr> localEnumeration = paramReference.getAll();
		for (int i = 0; localEnumeration.hasMoreElements(); i++) {
			RefAddr localRefAddr = (RefAddr) localEnumeration.nextElement();
			if (localRefAddr.getType().compareTo(paramString) == 0) {
				return i;
			}
		}
		throw new Exception("The \"" + paramString + "\" name/value pair was not found"
				+ " in the Reference object. The reference Object is" + " " + paramReference.toString());
	}
}
