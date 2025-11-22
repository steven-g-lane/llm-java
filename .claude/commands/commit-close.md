1. Commit all the open work locally.
2. Push committed code to remote.
3. Close the Github ticket or tickets you were working on and add the URL of the commit to your close comment.
	a. Remember the URL format: https://github.com/steven-g-lane/<repo-name>>/commit/<full-40-hash>
	b. Don't invent full hashes from the short forms i.e. don't do this: "When I saw bb3a527 in the git log output, I incorrectly extended it to make a full 40-character SHA"
  instead of getting the actual full hash. Git was showing me the abbreviated version, and I should have
   used git log --format="%H" to get the complete hash."