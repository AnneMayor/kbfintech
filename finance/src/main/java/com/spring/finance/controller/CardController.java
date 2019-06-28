package com.spring.finance.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.spring.finance.domain.AccountVO;
import com.spring.finance.domain.CardVO;
import com.spring.finance.mapper.AccountMapper;
import com.spring.finance.mapper.CardMapper;
import com.spring.finance.mapper.KAccountMapper;
import com.spring.finance.mapper.KCardMapper;
import com.spring.finance.util.LoginSessionTool;
import com.spring.finance.util.SHA256;

@Controller
public class CardController {

	@Autowired
	SqlSession sqlSession;

	@RequestMapping(value = "/payRegister", method = RequestMethod.GET)
	public ModelAndView pay(HttpSession session, Model model, HttpServletRequest request,
			HttpServletResponse response) {

		ModelAndView mv = new ModelAndView();

		AccountMapper AccountMapper = sqlSession.getMapper(AccountMapper.class);

		CardMapper CMapper = sqlSession.getMapper(CardMapper.class);

		LoginSessionTool.checkSession(session, model, response);

		try {

			String id = (String) session.getAttribute("id");

			// M_ID 받아와야 함.
			String ACCOUNT_NUMBER = AccountMapper.getAccountNum(id);
			mv.setViewName("payRegister/register");

			// 계좌가 등록 되어있지 않으면 카드도 등록해야 함.
			if (ACCOUNT_NUMBER == null) {
				mv.addObject("result", "allNeed");
				return mv;
			} else {
				// 카드가 등록 되어있지 않으면
				if (CMapper.isRegister("ske02154") == null) {
					mv.addObject("result", "card");
					return mv;
				} else {
					// 카드가 등록 되어있으면
					mv.addObject("result", "all");
					return mv;
				}
			}
		} catch (NullPointerException e) {

		}

		return mv;
	}

	// AJAX 사용시 Map<String, String> 만 가능
	@ResponseBody
	@RequestMapping(value = "/account/inquery", method = RequestMethod.POST)
	public Map<String, String> accountRegit(HttpSession session, Model model, HttpServletRequest request,
			HttpServletResponse response) {

		// 등록 성공하면
		String id = (String) session.getAttribute("id");

		KAccountMapper KAccountMapper = sqlSession.getMapper(KAccountMapper.class);
		AccountMapper AccountMapper = sqlSession.getMapper(AccountMapper.class);

		Map<String, String> m = new HashMap<String, String>();
		String accountName = request.getParameter("accountName");
		String accountNum = request.getParameter("accountNum");
		String phone = (String) session.getAttribute("phone");
		String accountMoney = KAccountMapper.getAccountMoney(accountNum);

		LoginSessionTool.checkSession(session, model, response);

		AccountVO accountVO = new AccountVO(id, accountName, accountNum, phone, Integer.parseInt(accountMoney));
		int cnt = AccountMapper.regitAccount(accountVO);

		// account 등록 실패
		if (cnt == 0) {
			m.put("resultCode", "0");
		} else {
			m.put("resultCode", "1");
		}

		return m;
	}

	// M_ID 넘겨 받아야함.
	// ajax 호출시 붙여야 하는 어노테이션
	@ResponseBody
	@RequestMapping(value = "/card/inquery", method = RequestMethod.POST)
	public Map<String, String> inquery(HttpSession session, Model model, HttpServletRequest request, HttpServletResponse response) {

		Map<String, String> m = new HashMap<String, String>();
		String cardNum = request.getParameter("cardNum");
		String cardMonth = request.getParameter("cardMonth");
		String cardYear = request.getParameter("cardYear");
		String cardCVC = request.getParameter("cardCVC");
		String selectComp = request.getParameter("selectComp");

		SHA256 sha = new SHA256();
		String shaResult = sha.testSHA256(cardNum + cardMonth + cardYear + cardCVC + selectComp);

		LoginSessionTool.checkSession(session, model, response);

		// kb 카드 해쉬 정보 가져오기
		KCardMapper KMapper = sqlSession.getMapper(KCardMapper.class);
		Map<String, String> map = new HashMap<String, String>();
		map.put("K_ID", cardNum);
		map.put("K_MM", cardMonth);
		map.put("K_YY", cardYear);
		map.put("K_CVC", cardCVC);

		String K_HASH = KMapper.getCardHash(map);
		ModelAndView mv = new ModelAndView();
		if (K_HASH.equals(shaResult)) {
			// card 등록
			// M_ID 받아오기
			String id = (String) session.getAttribute("id");
			CardMapper CMapper = sqlSession.getMapper(CardMapper.class);
			AccountMapper AccountMapper = sqlSession.getMapper(AccountMapper.class);
			// 계좌번호랑 카드 타입 가져와야함.
			String ACCOUNT_NUMBER = AccountMapper.getAccountNum(id);
			Map<String, String> map2 = new HashMap();
			map2.put("K_ID", cardNum);
			String CARD_TYPE = KMapper.getCardType(map2);
			CardVO CardVO = new CardVO(cardNum, cardMonth, cardYear, cardCVC, selectComp, shaResult, id,
					Integer.parseInt(CARD_TYPE), ACCOUNT_NUMBER);
			CMapper.regitCardInfo(CardVO);
			m.put("resultCode", "1");
		} else {
			m.put("resultCode", "0");
		}

		return m;
	}
}
