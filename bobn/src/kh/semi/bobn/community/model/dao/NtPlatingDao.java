package kh.semi.bobn.community.model.dao;

//Dao 에서 하는 일 : SQL문 실행
import static kh.semi.bobn.common.jdbc.JdbcDBCP.close;

import java.sql.*;
import java.util.ArrayList;

import kh.semi.bobn.community.model.vo.NtPlatingContentVo;
import kh.semi.bobn.community.model.vo.NtPlatingImgVo;
import kh.semi.bobn.community.model.vo.NtPlatingListVo;

public class NtPlatingDao {

	private PreparedStatement pstmt = null; // 질의
	private ResultSet rs = null; // 결과물을 가져옴

	// TODO : USER 회원가입 및 login 후 재수정
	// TODO : 임시 memberId : hyemi

	// 게시글등록
	public int insertPlatingContent(Connection conn, NtPlatingContentVo ntpcVo) {
		int result = 0;
		String memberId = "hyemi"; // TODO : 삭제
		System.out.println("PreparedStatement vo :" + ntpcVo);

//		String sql = "insert into ntpc(pb_no, pb_concept, pb_title, pb_content, pb_date, member_id)"
//				+ "values ((select nvl(max(pb_no),0)+1 from ntpc),?,?,?,default,(select member_id from member where member_id='"+memberId+"'))";

		String sql = "insert into ntpc(pb_no, pb_concept, pb_title, pb_content, member_id)"
				+ "values ((select nvl(max(pb_no),0)+1 from ntpc),?,?,?,?)";

		// vo에 가져온걸 sql문에 넣어줌
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, ntpcVo.getPbConcept());
			pstmt.setString(2, ntpcVo.getPbTitle());
			pstmt.setString(3, ntpcVo.getPbContent());
			pstmt.setString(4, memberId);

			// 실행시켜주고 결과를 result에 담아줌
			result = pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();

			// 다 사용하고 close
		} finally {
			close(pstmt);
		}

		// 담아놨던 결과 result를 리턴
		return result;

	}

	// 이미지등록
	public int insertPlatingImg(Connection conn, NtPlatingImgVo ntpiVo) {
		int result = 0;
		System.out.println("PreparedStatement vo :" + ntpiVo);

		String sql = "insert into ntpi(pi_no, pi_file, pb_no)"
				+ "values ((select nvl(max(pi_no),0)+1 from ntpi),?,(select pb_no from(select * from ntpc order by pb_date desc)where rownum=1))";

		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, ntpiVo.getPiFile());

			result = pstmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(pstmt);
		}

		return result;

	}
	//게시글 총 갯수 조회
		public int countPlatingList(Connection conn, String pbConcept) {
			int result = 0;
			String sql = "";
			
			if(pbConcept.equals("4")) {
				sql = "select count(*) from ntpc";
			}else {
				sql = "select count(*) from ntpc where pb_concept=?";
			}
			try {
				pstmt = conn.prepareStatement(sql);
				
				if(!pbConcept.equals("4")) {
					pstmt.setString(1, pbConcept);	
				}
				rs = pstmt.executeQuery();
				if(rs.next()) {
					result = rs.getInt(1);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				close(rs);
				close(pstmt);
			}
			
			return result;
		}

	// 게시글 조회(특정concept지정)
		public ArrayList<NtPlatingListVo> listPlatingContent(Connection conn, String pbConcept, int startRnum, int endRnum) {
			ArrayList<NtPlatingListVo> ntpcVolist = null;
			String sql = "";
			//컨셉 번호에 따라서 조회
			if(pbConcept.equals("4")) {
				sql = "select *"
						+ "from (select rownum r, ntpc.* from(select * from ntpc join(select ntpi.*from (select row_number() over(partition by ntpi.pb_no order by pb_no)as rnum,"
						+ "ntpi.* from ntpi) ntpi where rnum = 1)"
						+ "using(pb_no))ntpc) "
						+ "where r between ? and ?";
			}else {
				sql = "select *"
						+ "from (select rownum r, ntpc.* from(select * from ntpc join(select ntpi.*from (select row_number() over(partition by ntpi.pb_no order by pb_no)as rnum,"
						+ "ntpi.* from ntpi) ntpi where rnum = 1)"
						+ "using(pb_no))ntpc where  pb_concept = ? )"
						+ "where r between ? and ?";
			}
			// vo에 가져온걸 sql문에 넣어줌
			try {
				pstmt = conn.prepareStatement(sql);
				if(pbConcept.equals("4")) {
					pstmt.setInt(1, startRnum);
					pstmt.setInt(2, endRnum);
				}else {
					pstmt.setString(1, pbConcept);
					pstmt.setInt(2, startRnum);
					pstmt.setInt(3, endRnum);
				}
				rs = pstmt.executeQuery();
				ntpcVolist = new ArrayList<NtPlatingListVo>();
				// rs.next로 읽어서 받아온 결과 반복문돌리기
				while (rs.next()) {
					// ArrayList<NtPlatingContentVo>에서 <>안에 참조자료형이기 때문에 아래 new코드 필요
					NtPlatingListVo ntpcVo = new NtPlatingListVo();
					// db컬럼명 그대로 읽어서 ntpcVo에 넣어주기
					ntpcVo.setPbNo(rs.getInt("pb_no"));
					ntpcVo.setPbTitle(rs.getString("pb_title"));
					ntpcVo.setPbContent(rs.getString("pb_content"));
					ntpcVo.setPbConcept(rs.getString("pb_concept"));
					ntpcVo.setPiFile(rs.getString("pi_file"));
					ntpcVo.setMemberId(rs.getString("member_id"));
					

					// ntpcVolist에 ntpcVo담기
					ntpcVolist.add(ntpcVo);
					System.out.println("ntpcVolist :" + ntpcVolist);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				close(rs);
				close(pstmt);
			}
			// 담아놨던 결과 ntpcVolist를 리턴
			return ntpcVolist;

		}
		
		//게시글 상세조회
		public NtPlatingListVo detailPlatingContent(Connection conn, int pbNo) {
			NtPlatingListVo ntpcVolist = null;
			
//			String sql ="select rownum r, ntpc.* "
//					+ "from(select * from ntpc join(select ntpi.*from (select row_number() over(partition by ntpi.pb_no order by pb_no)as rnum,ntpi.* from ntpi) ntpi "
//					+ "where rnum = 1)using(pb_no))ntpc"
//					+ " where  pb_no = ?;";

			String sql = "select * from ntpc join ntpi using(pb_no) where pb_no=?";

			// vo에 가져온걸 sql문에 넣어줌
			try {
				pstmt = conn.prepareStatement(sql);
				pstmt.setInt(1, pbNo);
				rs = pstmt.executeQuery();

				ntpcVolist = new NtPlatingListVo();
				if(rs.next()) {
					ntpcVolist.setPbNo(rs.getInt("pb_no"));
					ntpcVolist.setPbTitle(rs.getString("pb_title"));
					ntpcVolist.setPbContent(rs.getString("pb_content"));
					ntpcVolist.setPiFile(rs.getString("pi_file"));
					ntpcVolist.setMemberId(rs.getString("member_id"));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				close(rs);
				close(pstmt);
			}

			// 담아놨던 결과 result를 리턴
			return ntpcVolist;

		}

}
